import os 
from ultralytics import YOLO 
print("Dışarıdan yüklenen görsel test başlıyor...")
print("Dataset dışından gelen görselle performans testi")
model =YOLO ('runs/detect/train2/weights/best.pt')
external_image ='test/images/asdffff.jpg'
if not os .path .exists (external_image ):
    print(f"HATA: {external_image} dosyası bulunamadı!")
    print("test/images/ klasöründe asdffff.jpg var mı kontrol et")
    exit ()
print(f"Görsel bulundu: {external_image}")
print("Model analiz ediyor...")
confidence_levels =[0.15 ,0.3 ,0.5 ]
for conf_threshold in confidence_levels :
    print(f"\n{'='*50}")
    print(f"Confidence Threshold: {conf_threshold}")
    print(f"{'='*50}")
    results =model .predict (
    source =external_image ,
    save =True ,
    show =False ,
    conf =conf_threshold ,
    verbose =False ,
    name =f'external_conf_{conf_threshold }'
    )
    detection_count =0 
    detections_by_class ={}
    for result in results :
        boxes =result .boxes 
        if boxes is not None :
            for box in boxes :
                class_name =result .names [int (box .cls )]
                confidence =box .conf .item ()
                print(f"   {class_name}: %{confidence * 100:.1f}")
                detection_count +=1 
                if class_name not in detections_by_class :
                    detections_by_class [class_name ]=[]
                detections_by_class [class_name ].append (confidence )
    if detection_count ==0 :
        print("   Bu confidence seviyesinde hiç tespit yok")
    else :
        print(f"\n   Toplam {detection_count} tespit")
        print("   Sınıf bazında:")
        for class_name ,confidences in detections_by_class .items ():
            avg_conf =sum (confidences )/len (confidences )
            print (f"      {class_name }: {len (confidences )} adet, ortalama %{avg_conf *100 :.1f}")
print(f"\n{'='*60}")
print("DIŞARIDAN GÖRSEL DEĞERLENDİRMESİ:")
print("Bu test dataset performansı ile karşılaştırma yapmanı sağlar")
print("\nSonuç görselleri:")
print ("   runs/detect/external_conf_0.1/")
print ("   runs/detect/external_conf_0.3/")
print ("   runs/detect/external_conf_0.5/")
