export default async function handler(req: any, res: any) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'POST only' });
  }

  const {
    action = 'chat',
    subject = 'ICMAP',
    topic = '',
    question = '',
    language = 'bilingual',
    difficulty = 'intermediate',
    count = 5,
    context = ''
  } = req.body || {};

  const lang =
    language === 'english'
      ? 'English only'
      : language === 'roman_urdu'
        ? 'Roman Urdu'
        : 'English + Urdu when helpful';

  let task = '';
  if (action === 'mcq') {
    task = `Create ${count} exam-style MCQs about ${topic}. Return ONLY valid JSON: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"..."}]}`;
  } else if (action === 'flashcards') {
    task = `Create ${count} active-recall flashcards about ${topic}. Return ONLY valid JSON: {"cards":[{"front":"...","back":"...","hint":"..."}]}`;
  } else if (action === 'plan') {
    task = `Create a realistic 5-step study plan for ${topic} for an ICMAP student. Return ONLY valid JSON: {"sessions":[{"title":"...","minutes":45,"task":"..."}],"strategy":"..."}`;
  } else if (action === 'numerical') {
    task = `Create ${count} numerical/accounting practice MCQs on ${topic}, with worked explanations. Return ONLY valid JSON: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"..."}]}`;
  } else if (action === 'typing') {
    task = `Create ${count} short-answer 5-mark ICMA exam questions on ${topic}. Return ONLY valid JSON: {"questions":[{"question":"...","keywords":["..."],"modelAnswer":"..."}]}`;
  } else if (action === 'summarize_bullets') {
    task = `Summarize the selected study passage into clear bullet-point exam notes with Core Idea, 4-6 Key Bullets, Key Terms, and Quick Memory Hook.`;
  } else if (action === 'explain_concept') {
    task = `Explain the complex concepts in the selected study passage in simple terms with Plain-English Meaning, Step-by-Step Breakdown, Practical Example, and Exam Watch-Out.`;
  } else {
    task = question || `Teach me ${topic}`;
  }

  const system = `You are ICMAP Smart Tutor, an expert study coach for Organizational Management (M3), Financial Accounting, Business Taxation, and Performance Management. Subject: ${subject}. Topic: ${topic}. Difficulty: ${difficulty}. Language: ${lang}. Use the supplied study context when present and stay accurate and exam-focused.`;
  const userMessage = context ? `${task}\n\nStudy context:\n${String(context).slice(0, 6000)}` : task;

  const openRouterKey = process.env.OPENROUTER_API_KEY;
  if (openRouterKey) {
    const model = process.env.OPENROUTER_MODEL || 'meta-llama/llama-3.3-8b-instruct:free';
    try {
      const r = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${openRouterKey}`,
          'Content-Type': 'application/json',
          'HTTP-Referer': process.env.APP_URL || 'https://icmap-portal.vercel.app',
          'X-Title': 'ICMAP Smart Tutor Portal'
        },
        body: JSON.stringify({
          model,
          messages: [
            { role: 'system', content: system },
            { role: 'user', content: userMessage }
          ],
          temperature: 0.35
        })
      });
      const data = await r.json();
      if (r.ok) {
        const text = data?.choices?.[0]?.message?.content || '';
        const parsed = tryParseJson(text);
        return res.json({ text, parsed, provider: 'openrouter' });
      }
    } catch {
      // Fall through to zero-key Pollinations free endpoint
    }
  }

  // Zero-key free fallback via Pollinations Open Text API
  try {
    const r = await fetch('https://text.pollinations.ai/', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json, text/plain'
      },
      body: JSON.stringify({
        model: 'openai',
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: userMessage }
        ]
      })
    });
    const rawText = await r.text();
    if (!r.ok) {
      return res.status(r.status).json({ error: `Free AI endpoint returned HTTP ${r.status}` });
    }
    let text = rawText.trim();
    if (text.startsWith('{')) {
      const maybeObj = tryParseJson(text);
      const choiceText = maybeObj?.choices?.[0]?.message?.content;
      if (choiceText) text = String(choiceText).trim();
    }
    const parsed = tryParseJson(text);
    return res.json({ text, parsed, provider: 'pollinations-free' });
  } catch (e: any) {
    return res.status(500).json({ error: e?.message || 'Tutor server error' });
  }
}

function tryParseJson(raw: string): any {
  try {
    const cleaned = raw
      .replace(/^\s*```json\s*/i, '')
      .replace(/^\s*```\s*/i, '')
      .replace(/\s*```\s*$/i, '')
      .trim();
    return JSON.parse(cleaned);
  } catch {
    return null;
  }
}