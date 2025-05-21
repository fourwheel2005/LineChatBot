from flask import Flask, request, jsonify
from pythainlp.tokenize import word_tokenize

app = Flask(__name__)

intent_keywords = {
    "contact": ["ติดต่อ", "เบอร์", "โทร", "ไลน์"],
    "price": ["ราคา", "บาท", "เท่าไหร่", "คิดยังไง"],
    "order": ["สั่งซื้อ", "สั่ง", "อยากได้", "จะซื้อ"]
}

@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json()
    text = data.get("text", "")
    tokens = word_tokenize(text, engine="newmm")

    for intent, keywords in intent_keywords.items():
        if any(word in tokens for word in keywords):
            return jsonify({"intent": intent})

    return jsonify({"intent": "unknown"})

if __name__ == "__main__":
    import os

    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)

