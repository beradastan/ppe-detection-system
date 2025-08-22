import os 

import json 

import logging 

BASE_DIR =os .path .dirname (os .path .abspath (__file__ ))

DB_PATH =os .path .normpath (os .path .join (BASE_DIR ,"..","database","ppe_logs.db"))

CONFIG_PATH =os .path .join (BASE_DIR ,"config.json")

CONFIG ={}

try :

    with open (CONFIG_PATH ,"r",encoding ="utf-8")as f :

        CONFIG =json .load (f )

except Exception as e :

    logging .getLogger (__name__ ).warning (f"config.json okunamadı veya bulunamadı: {e }")

JWT_SECRET =os .getenv ("JWT_SECRET","change_this_dev_secret")

JWT_ALGORITHM ="HS256"

JWT_EXPIRE_MINUTES =int (os .getenv ("JWT_EXPIRE_MINUTES","120"))

SERVER_CONFIG =CONFIG .get ("server",{})

SERVER_HOST =SERVER_CONFIG .get ("host","0.0.0.0")

SERVER_PORT =int (SERVER_CONFIG .get ("port",8000 ))

CORS_ORIGINS =SERVER_CONFIG .get ("cors_origins",[

"http://localhost:3000",

"http://127.0.0.1:3000",

"https://localhost:3000",

"https://127.0.0.1:3000",

"http://localhost:3001",

"https://localhost:3001"

])

MODEL_CONFIG =CONFIG .get ("model",{})

MODEL_PATH =MODEL_CONFIG .get ("path","../runs/detect/train2/weights/best.pt")

CONFIDENCE_THRESHOLD =float (MODEL_CONFIG .get ("confidence_threshold",0.5 ))

PPE_CONFIG =CONFIG .get ("ppe_rules",{})

REQUIRED_PPE =PPE_CONFIG .get ("required_items",['helmet','vest','boots'])

OPTIONAL_PPE =PPE_CONFIG .get ("optional_items",['glove','Dust Mask','Eye Wear','Shield'])

CLASS_NAMES =['Dust Mask','Eye Wear','Shield','boots','glove','helmet','person','vest']

logging .basicConfig (level =logging .INFO )

logger =logging .getLogger (__name__ )

