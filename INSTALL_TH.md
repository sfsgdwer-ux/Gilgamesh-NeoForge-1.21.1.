# Gilgamesh v1.4.0 — Ultimate Skill โดยตรง (ไม่ต้องใช้ KubeJS / TensurJS)

สำหรับ Minecraft Java 1.21.1 / NeoForge 21.1.265

## ติดตั้ง
- ปิดเกมและสำรองโลก
- นำไฟล์ `gilgamesh_tensura-1.4.0.jar` จากโฟลเดอร์ `mods` ใน ZIP ไปใส่โฟลเดอร์ `mods` ของเกม
- เอา Gilgamesh v1.2.x และ v1.3.0 ออกก่อน (ใช้เพียงรุ่นเดียว)
- ต้องมี **Tensura: Reincarnated 2.0.1.4** และ **ManasCore 4.0.0.2** กับ dependency ที่ Tensura ต้องใช้ตามปกติ
- **ไม่ต้องลง KubeJS หรือ TensurJS**
- หากเคยติดตั้งสคริปต์เก่า ให้ลบ `kubejs/startup_scripts/gilgamesh_tensura_skill.js` ไม่ให้เกิดสกิล ID ซ้ำ
- เปิดเกมใหม่

## ใช้สกิล
- บนโลกทดสอบที่เปิด cheats ใช้ `/gilgamesh grant` เพื่อเพิ่มสกิลให้ผู้เล่น
- เลือก `Gilgamesh, Lord of Treasures` ในหน้าสกิล Tensura และกดปุ่มใช้สกิลปกติค้าง
- ใช้ 200 Magicules ตอนเริ่มใช้
- ประตูทอง 5 วงตามหลังผู้เล่น ปล่อยชุดอาวุธแสดงผลจาก Ender Chest สูงสุด 16 ชิ้นต่อระลอก
- เมื่อปล่อยปุ่ม ประตูจะปิด
- `/gilgamesh count` ตรวจจำนวนช่องที่ใส่ของใน Ender Chest เท่านั้น

## ข้อจำกัด
Build ผ่านยังไม่เท่ากับทดสอบในเกมจริง ยังต้องตรวจการแสดงผล/การทำงานกับโลกของคุณ
