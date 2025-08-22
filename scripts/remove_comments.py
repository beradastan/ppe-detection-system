"""
remove_comments.py
Safely remove comments across a project with dry-run and backup support.
- Python: uses tokenize to remove comments while preserving strings and docstrings.
- Other text files: removes line/block comments by simple rules with safeguards for URLs (http://, https://) and data URIs.
Usage:
  python scripts/remove_comments.py --dry-run
  python scripts/remove_comments.py --apply [--backup]
  python scripts/remove_comments.py --include-ext .py .js .jsx .css .html .sh .kts .gradle .yaml .yml .properties .txt
  python scripts/remove_comments.py --exclude-dirs train valid test node_modules .git .gradle build dist .idea .vscode
By default, searches from repository root (this script's parent directory).
"""
from __future__ import annotations 
import argparse 
import os 
import sys 
import io 
import tokenize 
from typing import Iterable ,List ,Tuple ,Set 
DEFAULT_INCLUDE_EXTS :Tuple [str ,...]=(
".py",".js",".jsx",".css",".html",".sh",".kts",".gradle",
".yaml",".yml",".properties"
)
DEFAULT_EXCLUDE_DIRS :Tuple [str ,...]=(
".git","node_modules","build","dist",".gradle",".idea",".vscode"
)
URL_GUARDS =("http://","https://","ftp://","data:")
def is_binary (path :str )->bool :
    try :
        with open (path ,'rb')as f :
            chunk =f .read (2048 )
        if b"\0"in chunk :
            return True 
        chunk .decode ('utf-8')
        return False 
    except Exception :
        return True 
def read_text (path :str )->str :
    with open (path ,'r',encoding ='utf-8',errors ='strict')as f :
        return f .read ()
def write_text (path :str ,content :str ):
    with open (path ,'w',encoding ='utf-8',errors ='strict')as f :
        f .write (content )
def should_skip_dir (dirname :str ,exclude_dirs :Set [str ])->bool :
    name =os .path .basename (dirname )
    return name in exclude_dirs 
def iter_files (root :str ,include_exts :Set [str ],exclude_dirs :Set [str ])->Iterable [str ]:
    for base ,dirs ,files in os .walk (root ):
        dirs [:]=[d for d in dirs if not should_skip_dir (os .path .join (base ,d ),exclude_dirs )and d not in exclude_dirs ]
        for fn in files :
            path =os .path .join (base ,fn )
            if include_exts :
                if any (fn .endswith (ext )for ext in include_exts ):
                    yield path 
            else :
                yield path 
def strip_comments_python (src :str )->str :

    out =[]
    last_end =(1 ,0 )
    sio =io .StringIO (src )
    try :
        tokens =list (tokenize .generate_tokens (sio .readline ))
    except tokenize .TokenError :
        return src 
    for tok_type ,tok_str ,start ,end ,line in tokens :
        if tok_type ==tokenize .COMMENT :
            if start [1 ]==0 :
                out .append ("\n")
            last_end =end 
            continue 
        srow ,scol =start 
        lrow ,lcol =last_end 
        if srow >lrow :
            out .append ("\n"*(srow -lrow ))
            out .append (" "*scol )
        else :
            out .append (" "*(scol -lcol ))
        out .append (tok_str )
        last_end =end 
    return "".join (out )
def has_url_guard (line :str )->bool :
    return any (g in line for g in URL_GUARDS )
def strip_line_comment (line :str ,markers :Tuple [str ,...])->str :

    stripped =line 
    if stripped .startswith ("#!"):
        return line 
    for m in markers :
        idx =stripped .find (m )
        if idx !=-1 :
            if m =='//'and has_url_guard (stripped [:idx +2 ]):
                continue 
            prefix =stripped [:idx ]
            if prefix .count ('"')%2 ==1 or prefix .count ("'")%2 ==1 :
                continue 
            stripped =stripped [:idx ].rstrip ()+("\n"if stripped .endswith ("\n")else "")
    return stripped 
def strip_block_comments (text :str ,start :str ,end :str )->str :
    out =[]
    i =0 
    n =len (text )
    while i <n :
        s =text .find (start ,i )
        if s ==-1 :
            out .append (text [i :])
            break 
        out .append (text [i :s ])
        e =text .find (end ,s +len (start ))
        if e ==-1 :
            break 
        i =e +len (end )
    return "".join (out )
def strip_comments_generic (src :str ,ext :str )->str :
    text =src 
    if ext in {'.js','.jsx','.css','.kts','.gradle'}:
        text =strip_block_comments (text ,'/*','*/')
    if ext in {'.html'}:
        text =strip_block_comments (text ,'<!--','-->')
    line_markers :Tuple [str ,...]=tuple ()
    if ext in {'.js','.jsx','.css','.kts','.gradle'}:
        line_markers =('//',)
    elif ext in {'.sh','.yaml','.yml','.properties'}:
        line_markers =('#',)
    elif ext in {'.html'}:
        line_markers =tuple ()
    else :
        line_markers =('//','#')
    if line_markers :
        lines =text .splitlines (keepends =True )
        new_lines =[strip_line_comment (ln ,line_markers )for ln in lines ]
        text ="".join (new_lines )
    return text 
def process_file (path :str ,include_exts :Set [str ])->Tuple [bool ,str ]:
    ext =os .path .splitext (path )[1 ].lower ()
    if include_exts and ext not in include_exts :
        return False ,""
    if is_binary (path ):
        return False ,""
    try :
        original =read_text (path )
    except Exception :
        return False ,""
    if ext =='.py':
        new =strip_comments_python (original )
    else :
        new =strip_comments_generic (original ,ext )
    if new !=original :
        return True ,new 
    return False ,""
def main ():
    parser =argparse .ArgumentParser (description ="Remove comments across project.")
    parser .add_argument ('--root',default =None ,help ='Root directory to scan (default: repo root)')
    parser .add_argument ('--apply',action ='store_true',help ='Apply changes to files')
    parser .add_argument ('--dry-run',action ='store_true',help ='Show what would change without writing')
    parser .add_argument ('--backup',action ='store_true',help ='Create .bak backups before writing')
    parser .add_argument ('--include-ext',nargs ='*',default =list (DEFAULT_INCLUDE_EXTS ),help ='File extensions to include (e.g., .py .js)')
    parser .add_argument ('--exclude-dirs',nargs ='*',default =list (DEFAULT_EXCLUDE_DIRS ),help ='Directory names to exclude during scan')
    args =parser .parse_args ()
    if not args .apply and not args .dry_run :
        args .dry_run =True 
    root =args .root or os .path .abspath (os .path .join (os .path .dirname (__file__ ),os .pardir ))
    include_exts =set (e if e .startswith ('.')else f'.{e }'for e in args .include_ext )
    exclude_dirs =set (args .exclude_dirs )
    changed =0 
    scanned =0 
    modified_files :List [str ]=[]
    for path in iter_files (root ,include_exts ,exclude_dirs ):
        scanned +=1 
        did_change ,new_content =process_file (path ,include_exts )
        if did_change :
            changed +=1 
            modified_files .append (path )
            if args .apply :
                if args .backup :
                    try :
                        bak_path =path +'.bak'
                        if not os .path .exists (bak_path ):
                            with open (bak_path ,'w',encoding ='utf-8')as bf :
                                bf .write (read_text (path ))
                    except Exception :
                        pass 
                write_text (path ,new_content )
    print (f"Scanned files: {scanned }")
    print (f"Modified files: {changed }")
    if not args .apply :
        sample =modified_files [:20 ]
        if sample :
            print ("Sample modified files:")
            for p in sample :
                print (f" - {os .path .relpath (p ,root )}")
        else :
            print ("No changes would be made.")
    else :
        print ("Changes applied.")
if __name__ =='__main__':
    main ()
