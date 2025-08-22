import os 
import re 
def remove_comments_from_file (file_path ):
    try :
        with open (file_path ,'r',encoding ='utf-8')as file :
            content =file .read ()
        content =re .sub (r'#.*$','',content ,flags =re .MULTILINE )
        content =re .sub (r'"""[\s\S]*?"""','',content )
        content =re .sub (r"'''[\s\S]*?'''",'',content )
        lines =[line for line in content .split ('\n')if line .strip ()!='']
        with open (file_path ,'w',encoding ='utf-8')as file :
            file .write ('\n'.join (lines ))
        return True 
    except Exception as e :
        print (f"Error processing {file_path }: {str (e )}")
        return False 
def process_directory (directory ):
    for root ,_ ,files in os .walk (directory ):
        if 'venv'in root or '__pycache__'in root :
            continue 
        for file in files :
            if file .endswith ('.py'):
                file_path =os .path .join (root ,file )
                print (f"Processing: {file_path }")
                remove_comments_from_file (file_path )
if __name__ =="__main__":
    project_root =os .path .dirname (os .path .abspath (__file__ ))
    process_directory (project_root )
    print ("All comments have been removed from Python files.")
