import io 
import tokenize 
import os 

def remove_comments_from_file (filepath ):
    with open (filepath ,'r',encoding ='utf-8')as file :
        source =file .read ()

    result =[]
    prev_token =None 
    g =tokenize .generate_tokens (io .StringIO (source ).readline )

    for token in g :
        token_type =token [0 ]
        token_string =token [1 ]
        start_row ,start_col =token [2 ]
        end_row ,end_col =token [3 ]

        if token_type ==tokenize .COMMENT :
            continue 

        if token_type ==tokenize .STRING and (prev_token and prev_token [0 ]==tokenize .INDENT or token [0 ]==tokenize .OP and token [1 ]in ('(','[')):
            continue 

        result .append ((token_type ,token_string ))
        prev_token =token 

    output =tokenize .untokenize (result )

    with open (filepath ,'w',encoding ='utf-8')as file :
        file .write (output )

def process_directory (directory ):
    for root ,dirs ,files in os .walk (directory ):
        if 'venv'in root or '__pycache__'in root or '.git'in root :
            continue 

        for file in files :
            if file .endswith ('.py'):
                filepath =os .path .join (root ,file )
                print (f"Processing: {filepath }")
                try :
                    remove_comments_from_file (filepath )
                except Exception as e :
                    print (f"Error processing {filepath }: {e }")

if __name__ =="__main__":
    project_root =os .path .dirname (os .path .abspath (__file__ ))
    process_directory (project_root )
    print ("All comments have been removed from Python files.")
