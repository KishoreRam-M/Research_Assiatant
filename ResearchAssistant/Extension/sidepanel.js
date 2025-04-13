document.addEventListener("DOMContentLoaded", () => {
  // Load saved research notes from local storage
  chrome.storage.local.get(["researchNotes"], function (result) {
    if (result.researchNotes) {
      document.getElementById("input").value = result.researchNotes;
    }
  });

  // Summary button click
  document.querySelector(".summary").addEventListener("click", summarizeText);

  // Save button click
  document.querySelector(".save").addEventListener("click", saveNotes);
});

// Summarize selected text in the current tab
async function summarizeText() {
  try {
    const [tab] = await chrome.tabs.query({
      active: true,
      currentWindow: true,
    });

    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      function: () => window.getSelection().toString(),
    });

    if (!result) {
      showResult("⚠️ Please select some text on the current page first.");
      return;
    }

    const response = await fetch("http://localhost:8080/api/research/process", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: result, operations: "summarize" }), // 🔥 FIXED
    });

    if (!response.ok) {
      throw new Error(`API ERROR: ${response.status}`);
    }

    const text = await response.text();
    showResult(text.replace(/\n/g, "<br>"));
  } catch (error) {
    showResult(`❌ Error: ${error.message}`);
  }
}

// Display the summary result
function showResult(content) {
  let resultContainer = document.getElementById("result");
  if (!resultContainer) {
    resultContainer = document.createElement("div");
    resultContainer.id = "result";
    resultContainer.className = "result-content";
    document.querySelector(".ai").appendChild(resultContainer);
  }
  resultContainer.innerHTML = `<div class="result-content">${content}</div>`;
}

// Save notes to chrome local storage
function saveNotes() {
  const notes = document.getElementById("input").value;
  chrome.storage.local.set({ researchNotes: notes }, function () {
    alert("✅ Notes saved successfully!");
  });
}
