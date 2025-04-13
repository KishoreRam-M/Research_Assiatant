
**1. What is a Chrome Extension?**

* **Concept:** A Chrome extension is a small software program built using web technologies (HTML, CSS, JavaScript) that customizes and enhances the Google Chrome browser. Think of them as browser add-ons.
* **Purpose:** They can modify web pages, interact with browser features (like tabs, bookmarks, history), add buttons to the toolbar, or run scripts in the background.
* **Structure:** Every extension has a core file called `manifest.json`. This file tells Chrome essential information about the extension: its name, version, permissions it needs (like accessing tabs or storage), and which scripts or pages to load (like background scripts, content scripts, or popup pages).
* **Components (Common Types):**
    * **Popup:** A small HTML page that appears when you click the extension's icon in the toolbar. The code you provided is most likely the JavaScript for such a popup page.
    * **Content Scripts:** JavaScript files that run directly within the context of web pages the user visits. They can read or modify the page's content (DOM).
    * **Background Scripts:** Run in the background, independent of any specific web page or popup. They handle long-running tasks or events.
    * **Options Page:** An HTML page for users to configure the extension's settings.
* **Permissions:** Extensions must declare the permissions they need in the `manifest.json`. Users are shown these permissions upon installation. Examples: `tabs` (to interact with browser tabs), `storage` (to save data), `scripting` (to execute scripts on pages).

**2. How Does *This Specific* Extension Likely Work (Based on the Code)?**

This extension appears to be a research tool that allows users to:

1.  **Select text** on any webpage.
2.  **Summarize** that selected text using an AI service (running on a *local* server).
3.  **Take notes** within the extension's popup.
4.  **Save and load** those notes.

**Workflow:**

1.  The user clicks the extension's icon in the Chrome toolbar.
2.  A small window (the popup, defined by an HTML file linked in the `manifest.json`) appears. This HTML file includes the JavaScript code you provided.
3.  The JavaScript code immediately tries to load any previously saved notes (`researchNotes`) from local browser storage (`chrome.storage.local`) and display them in a text area (`#input`).
4.  The user can select text on the current webpage.
5.  The user clicks the "Summarize" button in the popup.
6.  The script gets the selected text from the active webpage (`chrome.scripting.executeScript`).
7.  It sends this text to a server running on the user's own computer (`http://localhost:8080/api/research/process`). **Important:** This means the summarization requires a separate backend application running locally.
8.  The local server processes the text (summarizes it) and sends the result back.
9.  The script displays the received summary in the popup (`#result` area).
10. The user can also type notes into the text area (`#input`).
11. Clicking the "Save" button saves the content of the text area into `chrome.storage.local`.

**3. Step-by-Step Code Explanation:**

```javascript
// Wait until the HTML structure of the popup page is fully loaded
document.addEventListener("DOMContentLoaded", () => {
  // --- Loading Saved Notes ---
  // Use the Chrome Extension Storage API (chrome.storage.local)
  // to retrieve data saved under the key "researchNotes".
  // chrome.storage.local is specific to the extension and persists across browser restarts.
  chrome.storage.local.get(["researchNotes"], function (result) {
    // This function is a callback that runs *after* Chrome tries to get the data.
    // 'result' is an object containing the retrieved data.
    // Example: if notes were saved, result might be { researchNotes: "My saved notes" }

    // Check if the 'researchNotes' key actually existed in the storage
    if (result.researchNotes) {
      // If notes were found, find the HTML element with the id "input"
      // (likely a <textarea>) and set its value to the loaded notes.
      document.getElementById("input").value = result.researchNotes;
    }
    // If result.researchNotes is null or undefined, the input field remains empty.
  });

  // --- Setting up the "Summarize" Button ---
  // Find the first HTML element with the class "summary" (likely a <button>).
  // Attach an event listener that calls the 'summarizeText' function when the element is clicked.
  document.querySelector(".summary").addEventListener("click", summarizeText);

  // --- Setting up the "Save" Button ---
  // Find the first HTML element with the class "save" (likely a <button>).
  // Attach an event listener that calls the 'saveNotes' function when the element is clicked.
  document.querySelector(".save").addEventListener("click", saveNotes);
}); // End of DOMContentLoaded listener

// --- Summarize Function ---
// 'async' indicates this function performs asynchronous operations (like waiting for API calls or Chrome functions)
async function summarizeText() {
  // 'try...catch' block handles potential errors during the process.
  try {
    // --- Get the Active Tab ---
    // Use the Chrome Tabs API (chrome.tabs) to find the currently active tab in the current browser window.
    // 'await' pauses the function until chrome.tabs.query finishes and returns the tab info.
    // '{ active: true, currentWindow: true }' specifies the criteria for the tab search.
    // 'const [tab]' destructures the result array to get the first (and likely only) tab object found.
    const [tab] = await chrome.tabs.query({
      active: true,
      currentWindow: true,
    });

    // --- Get Selected Text from the Tab ---
    // Use the Chrome Scripting API (chrome.scripting) to execute code *inside* the active web page.
    // 'await' pauses the function until the script execution completes and returns a result.
    const [{ result }] = await chrome.scripting.executeScript({
      // Specify the target tab where the script should run.
      target: { tabId: tab.id },
      // Define the function to execute within the web page's context.
      // 'window.getSelection().toString()' is standard JavaScript to get the currently highlighted text on a page.
      function: () => window.getSelection().toString(),
    });
    // The result from executeScript is an array of objects, one per frame the script ran in.
    // We assume it ran in the main frame and destructure to get the 'result' property, which holds the selected text.

    // --- Check if Text Was Selected ---
    // If 'result' is empty (null, undefined, or an empty string), no text was selected.
    if (!result) {
      // Call 'showResult' to display a warning message in the popup's result area.
      showResult("⚠️ Please select some text on the current page first.");
      // Stop the function execution here.
      return;
    }

    // --- Call the Local Summarization API ---
    // Use the standard 'fetch' API to send an HTTP request.
    // 'await' pauses the function until the server responds.
    const response = await fetch("http://localhost:8080/api/research/process", { // Target URL: A server running on the user's machine
      method: "POST", // Use the POST method to send data.
      headers: { "Content-Type": "application/json" }, // Indicate the request body format is JSON.
      // Create the request body as a JSON string:
      body: JSON.stringify({ content: result, operations: "summarize" }), // Send the selected text ('result') and the desired operation. // 🔥 FIXED - This line correctly formats the data to be sent.
    });

    // --- Check API Response Status ---
    // 'response.ok' is true if the HTTP status code is in the success range (e.g., 200-299).
    if (!response.ok) {
      // If the response status indicates an error (e.g., 404 Not Found, 500 Internal Server Error),
      // throw an error to be caught by the 'catch' block below.
      throw new Error(`API ERROR: ${response.status}`); // Include the status code in the error message.
    }

    // --- Process Successful API Response ---
    // 'await response.text()' gets the response body as plain text. We assume the server sends the summary this way.
    const text = await response.text();
    // Call 'showResult' to display the summary.
    // Replace newline characters ('\n') in the summary with HTML line break tags ('<br>')
    // so the summary displays with correct line breaks in the HTML popup.
    showResult(text.replace(/\n/g, "<br>"));

  } catch (error) {
    // --- Error Handling ---
    // If any error occurred in the 'try' block (e.g., network issue, API error, scripting error),
    // this 'catch' block will execute.
    // Call 'showResult' to display an error message in the popup.
    showResult(`❌ Error: ${error.message}`); // Show the specific error message.
  }
} // End of summarizeText function

// --- Display Result Function ---
// This function updates the popup's UI to show messages (summaries, errors, warnings).
function showResult(content) {
  // Find the HTML element with the id "result" (where results should be displayed).
  let resultContainer = document.getElementById("result");

  // If the result container doesn't exist yet (maybe the first time showing a result), create it.
  if (!resultContainer) {
    resultContainer = document.createElement("div"); // Create a new <div> element.
    resultContainer.id = "result"; // Set its ID.
    resultContainer.className = "result-content"; // Set its class (for styling).
    // Find the element with class "ai" (presumably the parent container for AI results)
    // and add the newly created resultContainer inside it.
    document.querySelector(".ai").appendChild(resultContainer);
  }
  // Set the inner HTML of the result container.
  // Wrap the actual 'content' inside another div with class 'result-content' (perhaps for consistent styling).
  // The 'content' variable already contains the text or HTML (like the summary with <br> tags or error messages).
  resultContainer.innerHTML = `<div class="result-content">${content}</div>`;
} // End of showResult function

// --- Save Notes Function ---
function saveNotes() {
  // Find the HTML element with the id "input" (the <textarea> for notes)
  // and get its current value (the text typed by the user).
  const notes = document.getElementById("input").value;

  // Use the Chrome Storage API to save data.
  // 'chrome.storage.local.set' saves data persistently for the extension.
  // Provide an object where keys are the names to save under ("researchNotes")
  // and values are the data to save (the 'notes' variable content).
  chrome.storage.local.set({ researchNotes: notes }, function () {
    // This callback function runs *after* the save operation completes successfully.
    // Show a simple browser alert box confirming the save.
    alert("✅ Notes saved successfully!");
  });
} // End of saveNotes function
```

