# วิธีติดตั้ง Gilgamesh v1.3.0 — Ultimate Skill (Minecraft 1.21.1 NeoForge)

ไฟล์ใน ZIP จัดตามโฟลเดอร์ Minecraft:
- `mods/gilgamesh_tensura-1.3.0.jar` (ชื่อไฟล์อาจต่างเล็กน้อย)
- `kubejs/startup_scripts/gilgamesh_tensura_skill.js`

1. ปิด Minecraft และสำรองโลกเดิม
2. แตกไฟล์ ZIP แล้ว **คัดลอกโฟลเดอร์ mods และ kubejs** ไปยังโฟลเดอร์เกม `.minecraft` (หรือไดเรกทอรีอินสแตนซ์ที่คุณใช้)
3. ลบ `gilgamesh_tensura` รุ่นเก่าจาก `mods` (อย่าลบ Tensura/Mysticism)
4. ติดตั้ง/คงไว้: NeoForge 21.1.265, Tensura 2.0.1.4, ManasCore, KubeJS, TensurJS `tensura_kubejs` และ dependency ม็อดตามปกติ
5. **ปิดแล้วเปิดเกมใหม่** ทุกครั้งที่เปลี่ยน `kubejs/startup_scripts` (พิมพ์ /reload ไม่พอ)
6. ในโลกที่เปิดคำสั่งได้ ลองคำสั่ง:
   `/tensura edit ability grant @s gilgamesh_tensura:gilgamesh`
   ถ้า autocomplete แสดงลำดับแตกต่าง ให้พิมพ์ `/tensura edit` แล้วเลือกเมนู ability -> grant ตามที่เกมแสดง
7. เปิดเมนูสกิลของ Tensura เลือก **Gilgamesh, Lord of Treasures** (Ultimate) แล้วกดปุ่มสกิลค้าง
8. ใส่ดาบ/ขวานหรือไอเทมที่ต้องการใน Ender Chest 1–27 ช่อง คลังไม่ถูกลบ
9. สกิลใช้ Magicules **200 หน่วยเฉพาะตอนเริ่มกด** และเปิดประตูมิติ 5 วง / ยิงทีละชุดสูงสุด 16 ชิ้น
10. ทดสอบจำนวนอาวุธได้ด้วย `/gilgamesh count` (ไม่ใช่คำสั่งใช้สกิล)

**สกิลนี้เป็นรายการใหม่** ID `gilgamesh_tensura:gilgamesh` ไม่ใช่การแทนชื่อสกิลของ Mysticism
**ไม่มีปุ่ม J แยก** ต้องใช้สกิลที่ได้รับใน Tensura เท่านั้น
**ยังไม่ได้ทดสอบในไคลเอนต์เกมจริง:** ได้ตรวจคอมไพล์ Java และ syntax JS ผ่าน Actions แต่การปรากฏใน UI/การรับสกิลต้องทดสอบในเกม
