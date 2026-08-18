// Small helper functions that call the Spring Boot backend.
// package.json has "proxy": "http://localhost:8080", so during
// `npm start` these relative paths are automatically forwarded there.
//
// IMPORTANT: none of the endpoints, request bodies, or response shapes here
// were changed for the UI redesign - this file talks to the exact same
// backend contract as before (POST /api/documents/upload, GET
// /api/documents, POST /api/questions/ask). uploadPdf() just gained an
// optional onProgress callback, implemented with XMLHttpRequest instead of
// fetch, purely so the upload UI can show a real progress bar - the actual
// HTTP request it sends is identical to before.

export function uploadPdf(file, onProgress) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append("file", file);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/documents/upload");

    if (typeof onProgress === "function") {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          onProgress(Math.round((event.loaded / event.total) * 100));
        }
      };
    }

    xhr.onload = () => {
      let data = {};
      try {
        data = JSON.parse(xhr.responseText);
      } catch {
        reject(new Error("Unexpected response from server"));
        return;
      }

      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(data);
      } else {
        reject(new Error(data.message || data.error || "Upload failed"));
      }
    };

    xhr.onerror = () => reject(new Error("Network error while uploading"));

    xhr.send(formData);
  });
}

export async function askQuestion(question) {
  const response = await fetch("/api/questions/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to get an answer");
  }
  return data;
}

// Fetches the list of documents already indexed in Qdrant, straight from
// the backend - this is what makes the sidebar survive a page refresh
// instead of forgetting everything that isn't in React state anymore.
// Also doubles as the backend "health check" - if this call fails, the
// topbar's online/offline indicator reflects that.
export async function fetchDocuments() {
  const response = await fetch("/api/documents");
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to load documents");
  }
  return data;
}
