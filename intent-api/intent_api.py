from flask import Flask, request, jsonify
from pythainlp.tokenize import word_tokenize
import os

app = Flask(__name__)

# หมวด intent และ keyword
intent_keywords = {
    "contact": [
        "ติดต่อ", "เบอร์", "โทร", "ไลน์", "ช่องทาง", "ติดต่อได้ที่", "หมายเลข", "ติดต่อกลับ", "สอบถาม",
        "แอดไลน์", "แชท", "แอด", "ส่งข้อความ", "คุยกับแอดมิน", "ติดต่อแอด", "ติดต่อร้าน", "ทางไหน",
        "แชทหา", "line", "ช่องทางการติดต่อ", "แอดไลน์ได้ที่ไหน", "มีเบอร์มั้ย"
    ],
    "price": [
        "ราคา", "บาท", "เท่าไหร่", "เท่าไร", "คิดยังไง", "คิดราคา", "ค่าส่ง", "แพงไหม", "โปรโมชั่น", "ลด", "เรท",
        "เท่าไหร่คะ", "เท่าไหร่ครับ", "ราคาเท่าไหร่", "ราคารวม", "บอกราคา", "สอบถามราคา", "ราคาชิ้นนี้",
        "ค่าบริการ", "ขอราคาด้วย", "ราคาต่อชิ้น", "ราคาทั้งหมด", "ราคาส่ง", "ราคาโปรโมชั่น"
    ],
    "order": [
        "สั่งซื้อ", "สั่ง", "อยากได้", "จะซื้อ", "สนใจ", "สั่งของ", "ขอสั่ง", "ขอซื้อ", "รับสินค้า", "ต้องการ",
        "สั่งเลย", "ซื้อเลย", "จัดส่ง", "ขอใบเสนอราคา", "สั่งยังไง", "สั่งได้ที่ไหน", "มีของมั้ย", "มีไหม",
        "อยากสั่ง", "สั่งตรงไหน", "กดสั่ง", "สั่งด่วน", "ขอรายละเอียด", "มีขายมั้ย", "รับเลย",
        "สั่งเลยได้ไหม", "ซื้อยังไง", "ต้องทำไงถึงสั่งได้"
    ]
}

# Endpoint วิเคราะห์ intent
@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json(force=True)
    text = data.get("text", "").strip()

    if not text:
        return jsonify({"intent": "unknown"})

    tokens = word_tokenize(text, engine="newmm")
    print(f"Tokens: {tokens}")  # DEBUG

    for intent, keywords in intent_keywords.items():
        if any(word in tokens for word in keywords):
            return jsonify({"intent": intent})

    return jsonify({"intent": "unknown"})

# เพิ่ม /ping endpoint สำหรับ health check
@app.route("/ping", methods=["GET"])
def ping():
    return "OK", 200

# Run app
if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
