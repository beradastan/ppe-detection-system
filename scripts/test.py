import os 
import random 
from ultralytics import YOLO 
print("Model rastgele test başlıyor...")
model =YOLO ('runs/detect/train2/weights/best.pt')
test_dir ='test/images/'
all_files =[f for f in os .listdir (test_dir )if f .lower ().endswith (('.jpg','.jpeg','.png'))]
random_files =random .sample (all_files ,min (5 ,len (all_files )))
print(f"Toplam {len(all_files)} test görseli var")
print("Rastgele seçilen 5 görsel:")
for i ,file in enumerate (random_files ,1 ):
    print (f"   {i }. {file [:50 ]}...")
print ("\n"+"="*60 )
total_detections =0 
class_counts ={}
for i ,filename in enumerate (random_files ,1 ):
    print(f"\nTest {i}/5: {filename}")
    test_path =os .path .join (test_dir ,filename )
    results =model .predict (
    source =test_path ,
    save =True ,
    show =False ,
    conf =0.3 ,
    verbose =False 
    )
    print("   Tespit sonuçları:")
    image_detections =0 
    for result in results :
        boxes =result .boxes 
        if boxes is not None :
            for box in boxes :
                class_name =result .names [int (box .cls )]
                confidence =box .conf .item ()
                print(f"      {class_name}: %{confidence * 100:.1f}")
                image_detections +=1 
                total_detections +=1 
                class_counts [class_name ]=class_counts .get (class_name ,0 )+1 
    if image_detections ==0 :
        print("      Hiç obje tespit edilmedi")
    else :
        print(f"      Bu görselde {image_detections} obje tespit edildi")
print ("\n"+"="*60 )
print("\nGENEL ANALİZ:")
print(f"   Toplam tespit: {total_detections}")
print(f"   Görsel başına ortalama: {total_detections / 5:.1f}")
print("\n   Sınıf dağılımı:")
for class_name ,count in sorted (class_counts .items ()):
    percentage =(count /total_detections *100 )if total_detections >0 else 0 
    print (f"      {class_name }: {count } adet (%{percentage :.1f})")
print("\n   Yeni rastgele test için: python random_test.py")
print("   Tüm sonuçlar: runs/detect/predict/ klasöründe")
