# ICMAP Smart Tutor Portal (PWA — Laptop & Mobile)

Responsive installable Progressive Web App for laptop, tablet, and phone featuring:
- **Bundled M3 Organizational Management Textbook (254 pages) & ML-1 Past Papers** (`/public/assets/aistudio/*.pdf`) with embedded PDF viewer + chapter quick-jumps
- **Voice Messages & Spoken Explanations**: tap the Mic button to ask questions by voice and hear answers read aloud in English or Urdu (`SpeechRecognition` + `speechSynthesis`)
- **Text-Selection AI Tutor**: highlight any passage to generate bullet-point notes or plain-English concept explanations
- **Digital Flashcards**: 3D flip, shuffle, keyboard navigation (`Space` / `←` / `→`), and AI card generation from reading materials
- **Pomodoro Focus Timer**: configurable work/break intervals + full-screen focus mode
- **Visual Study Roadmap**: milestone completion tracking across Units 1–14
- **Automatic Free AI Failover (Never Runs Out of Tokens)**:
  1. Server `GEMINI_API_KEY` (Google AI Studio `gemini-2.5-flash` -> `gemini-2.5-flash-lite` automatic fallback)
  2. Server `OPENROUTER_API_KEY` (optional free-tier backup)
  3. Built-in `$0` zero-key **Pollinations Open AI**, **Wikipedia REST Summary API**, and **Free Dictionary API**

## Setting Your Own Google AI Studio Key (Optional for Vercel `/api/tutor.ts`)
1. Visit [Google AI Studio](https://aistudio.google.com/apikey) and sign in with your Google account.
2. Click **Create API key** and copy your key (`AIza...`).
3. In **Vercel Project Settings → Environment Variables**, add `GEMINI_API_KEY` (and optionally `GEMINI_MODEL=gemini-2.5-flash`). Never paste private API keys into frontend client files.

## Deploying & Installing on Laptop / Phone
1. Deploy this repository to **Vercel** (uses `vercel.json` + `/api/tutor.ts` + `/api/youtube.ts`) or any static host (**Netlify / GitHub Pages** via `npm install && npm run build`).
2. Open the deployed HTTPS URL in **Chrome / Edge on Laptop** (click **Install PWA** in header or address bar) or **Chrome / Safari on Mobile** (**Add to Home Screen**).