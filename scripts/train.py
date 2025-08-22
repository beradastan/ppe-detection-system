from ultralytics import YOLO 
model =YOLO ('runs/detect/train2/weights/last.pt')
results =model .train (
data ='../config/data.yaml',
epochs =40 ,
batch =8 ,
imgsz =640 ,
patience =10 ,
device ='mps',
workers =0 ,
resume =True ,
)
print("Eğitim tamamlandı.")
