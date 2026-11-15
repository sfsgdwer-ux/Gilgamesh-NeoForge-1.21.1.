# ติดตั้ง Gilgamesh v1.5.0-dev (รุ่นทดลอง)

**Minecraft Java 1.21.1 / NeoForge 21.1.265**

## วิธีติดตั้ง
1. สำรองโลก และปิด Minecraft
2. ดาวน์โหลด ZIP จาก GitHub Actions แล้วแตกไฟล์
3. นำไฟล์ `gilgamesh_tensura-1.5.0-dev.jar` ที่อยู่ในโฟลเดอร์ `mods` ไปไว้ในโฟลเดอร์ `mods` ของอินสแตนซ์ Minecraft
4. นำ Gilgamesh รุ่นเก่าทุกตัวออก เพื่อไม่ให้ Mod ID ซ้ำ
5. ใช้ Tensura: Reincarnated **2.0.1.4**, ManasCore **4.0.0.2** และ dependency ที่เกมเดิมต้องใช้
6. **ไม่ต้องติดตั้ง KubeJS หรือ TensurJS** หากมีสคริปต์ v1.3.0 เก่า ให้ลบ `kubejs/startup_scripts/gilgamesh_tensura_skill.js`
7. เปิดเกมใหม่

## ทดสอบสกิล
1. ใช้โลกทดสอบที่เปิด Cheats และคำสั่ง `/gilgamesh grant` เพื่อรับ Ultimate Skill ใหม่
2. เปิดหน้าสกิล Tensura แล้วเลือก **กิลกาเมช ราชาแห่งคลังสมบัติ**
3. เปลี่ยนโหมดสกิลในเมนู Tensura ตามความสามารถที่ต้องการ
4. โหมด King's Storehouse ใช้หน้าคลัง **5 คอลัมน์ x 4 แถว (20 ช่อง)** แยกจาก Ender Chest
5. โหมด Territory Creation ต้องกดค้างและใช้ Max MP 1,000,000 แบบถาวร
6. คำสั่ง `/gilgamesh count` ตรวจจำนวนช่องที่มีไอเทมซึ่งระบบยอมรับในคลัง
7. ระบบ Mastery สูงสุด 2,000

## ความสามารถและข้อจำกัด
รุ่นนี้ลงทะเบียนเป็น Ultimate Skill ใน ManasCore โดยตรงและมีโหมดทั้งหมด 8 โหมด
ค่าเริ่มต้นสำหรับ Gate of Babylon 25,000 MP และ 5,000 MP/วินาที; Enkidu 50,000 MP
Ea ใช้ Max AP 1,000,000; Territory ใช้ Max MP 1,000,000; Enuma Elish ใช้ 100,000 MP/วินาที
Golden Rule มอบ Luck 5 ระหว่างที่สกิลถูกประมวลผล

**สถานะ: Experimental** — Gradle Build ผ่าน แต่ยังไม่ได้เปิดเกมทดสอบจริง 
ยังไม่มีวิวัฒนาการจาก Gatekeeper, การหัก MP 750,000 ตอนเรียนสกิลตามระบบ Mysticism, และความสามารถ Genesis ทั้งหมด
ทดสอบกับโลกสำรองก่อน และตรวจไฟล์ `latest.log` หากเกมค้างหรือไม่พบสกิล
