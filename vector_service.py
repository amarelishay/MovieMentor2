from flask import Flask, request, jsonify
import numpy as np
import faiss
import uuid
import os

app = Flask(__name__)

# ---- שלב 1: הגדרת מבנה הנתונים שלנו ----
# FAISS דורש אינדקס קבוע ממדי. נניח שכל embedding באורך 384
DIM = 384
index = faiss.IndexFlatL2(DIM)  # אינדקס שמבצע חיפוש לפי מרחק L2
user_metadata = {}  # מילון לשמירת metadata לפי vector-id (uuid)


# ---- שלב 2: אחסון embedding של משתמש ----
@app.route('/store_user_vector', methods=['POST'])
def store_user_vector():
    data = request.get_json()
    user_id = data['user_id']
    vector = np.array(data['embedding']).astype('float32')
    meta = data.get('metadata', {})

    if vector.shape[0] != DIM:
        return jsonify({"error": "Invalid embedding size"}), 400

    vector = np.expand_dims(vector, axis=0)
    index.add(vector)

    vector_uuid = str(uuid.uuid4())  # מזהה פנימי לוקטור
    user_metadata[vector_uuid] = {"user_id": user_id, **meta}

    return jsonify({"status": "stored", "vector_id": vector_uuid})


# ---- שלב 3: שליפת משתמשים דומים ----
@app.route('/find_similar_users', methods=['POST'])
def find_similar_users():
    data = request.get_json()
    query_vector = np.array(data['embedding']).astype('float32')
    top_k = int(data.get('top_k', 5))

    if index.ntotal == 0:
        return jsonify([])

    query_vector = np.expand_dims(query_vector, axis=0)
    distances, indices = index.search(query_vector, top_k)

    results = []
    for i in indices[0]:
        if i < len(user_metadata):
            meta = list(user_metadata.values())[i]
            results.append(meta)

    return jsonify(results)


# ---- שלב 4: בדיקת LIVE ----
@app.route('/')
def health_check():
    return jsonify({"status": "✅ Vector Service is live!"})


# ---- שלב 5: הפעלה ----
if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5005))  # Render יספק PORT מהסביבה
    app.run(host='0.0.0.0', port=port)
