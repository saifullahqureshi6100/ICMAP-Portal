# ICMAP Smart Tutor Portal (PWA — Laptop & Mobile)

Responsive installable Progressive Web App for laptop, tablet, and phone featuring:
- **Bundled M3 Organizational Management Textbook (254 pages) & ML-1 Past Papers** (`/public/assets/aistudio/*.pdf`) with embedded PDF viewer + chapter quick-jumps
- **Text-Selection AI Tutor**: highlight any passage to generate bullet-point notes or plain-English concept explanations
- **Digital Flashcards**: 3D flip, shuffle, keyboard navigation (`Space` / `←` / `→`), and AI card generation from reading materials
- **Pomodoro Focus Timer**: configurable work/break intervals + full-screen focus mode
- **Visual Study Roadmap**: milestone completion tracking across Units 1–14
- **Zero-Key Free AI (`$0`)**: works out of the box via Pollinations Open Text API, Wikipedia REST Summary API, and Free Dictionary API (with optional `OPENROUTER_API_KEY` and `YOUTUBE_API_KEY` on Vercel `/api/*`)

## Deploying & Installing on Laptop / Phone
1. Deploy this repository to **Vercel** (uses `vercel.json` + `/api/tutor.ts` + `/api/youtube.ts`) or any static host (**Netlify / GitHub Pages** via `npm install && npm run build`).
2. Open the deployed HTTPS URL in **Chrome / Edge on Laptop** (click **Install PWA** in header or address bar) or **Chrome / Safari on Mobile** (**Add to Home Screen**).