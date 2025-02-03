const express = require("express");
const puppeteer = require("puppeteer");
const multer = require("multer");
const fs = require("fs");
const path = require("path");

const app = express();
const upload = multer({ dest: "uploads/" });

// Ensure "Generated PDFs" directory exists
const pdfDir = path.join(__dirname, "Generated PDFs");
if (!fs.existsSync(pdfDir)) {
  fs.mkdirSync(pdfDir, { recursive: true });
}

app.post("/convert", upload.single("file"), async (req, res) => {
  try {
    if (!req.file) {
      console.log("❌ No file received!");
      return res.status(400).json({ error: "No file uploaded" });
    }

    console.log("✅ File received:", req.file.originalname);

    const htmlContent = fs.readFileSync(req.file.path, "utf8");
    const browser = await puppeteer.launch();
    const page = await browser.newPage();

    await page.setViewport({ width: 1280, height: 900 });
    await page.setContent(htmlContent, { waitUntil: "networkidle0" });

    await new Promise((resolve) => setTimeout(resolve, 1000));

    const pdfBuffer = await page.pdf({
      format: "Letter",
      printBackground: true,
    });

    const timestamp = new Date().toISOString().replace(/:/g, "_");
    const pdfPath = path.join(pdfDir, `converted_${timestamp}.pdf`);

    fs.writeFileSync(pdfPath, pdfBuffer);

    await browser.close();
    fs.unlinkSync(req.file.path); // Clean up uploaded file

    console.log("📄 PDF Generated Successfully at:", pdfPath);

    // Send a proper JSON response
    return res.status(200).json({
      message: "PDF generated successfully",
      filePath: pdfPath,
    });
  } catch (error) {
    console.error("🔥 Error:", error);

    return res.status(500).json({
      error: "Error generating PDF",
      details: error.message,
    });
  }
});

// Start the Node.js service
const PORT = 5000;
app.listen(PORT, () => {
  console.log(`Puppeteer service running on http://localhost:${PORT}`);
});
