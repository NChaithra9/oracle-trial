// Small helper functions that call the Spring Boot backend.
// package.json has "proxy": "http://localhost:8080", so during
// `npm start` these relative paths are automatically forwarded there.

export async function uploadPdf(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch("/api/documents/upload", {
    method: "POST",
    body: formData,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || data.error || "Upload failed");
  }
  return data;
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
