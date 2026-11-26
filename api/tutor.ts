export default async function handler(req: any, res: any) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'POST only' });
  }

  const {
    action = 'chat',
    subject = 'ICMA M3 Organizational Management',
    topic = '',
    question = '',
    language = 'urdu_bilingual',
    difficulty = 'icma_merit',
    count = 5,
    context = ''
  } = req.body || {};

  const langRule =
    language === 'english'
      ? 'Respond in crisp, professional English.'
      : 'Respond in natural, encouraging spoken Urdu (Roman Urdu / clear Urdu script) mixed with essential ICMA English technical terms so an executive finance student understands effortlessly when read or spoken aloud.';

  let task = '';
  if (action === 'mcq') {
    task = `Create ${count} high-standard MCQs calibrated to ICMA Pakistan M3 OM, ACCA BT/PM, CIMA E1, and ICAEW standards on "${topic}". Return ONLY valid JSON: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"...","topicTag":"${topic}"}]}`;
  } else if (action === 'flashcards') {
    task = `Create ${count} high-yield spaced-repetition flashcards on "${topic}" for today's post-office review. Return ONLY valid JSON: {"cards":[{"front":"...","back":"...","hint":"..."}]}`;
  } else if (action === 'plan' || action === 'daily_brief') {
    task = `Deliver today's full-day Emergency 9-Day Zero-to-Merit Strategist & Recovery Briefing for a working Finance executive with ZERO prior preparation whose ICMA M3 Organizational Management (OM-only) exam is on 5 December 2026. Include total study hours, office-break routine (08:00, 12:45 break + Short Test, 15:30 break, 17:30 tired-window 5 flashcards, 19:00 deep block, 20:00 15m recovery, 20:15 daily 60/40 test, 23:30 sleep), upcoming weekly/portion/mock alerts, study techniques, and emotional encouragement in ${langRule}. Context: ${topic}`;
  } else if (action === 'numerical') {
    task = `Create ${count} analytical & numerical OM/Performance Management MCQs (OEE, Variance, EOQ, Queueing, Ratios) on "${topic}". Return ONLY valid JSON: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"...","topicTag":"${topic}"}]}`;
  } else if (action === 'typing') {
    task = `Create ${count} 5-mark ICMA ML-1 / CIMA / ACCA descriptive typing exam questions on "${topic}". Return ONLY valid JSON: {"questions":[{"question":"...","marks":5,"topicTag":"${topic}","keywords":["..."],"modelAnswer":"..."}]}`;
  } else if (action === 'grade_typing') {
    task = `Evaluate the student's typed 5-mark ICMA answer below. Give marks out of 5, point out missing ICMA keywords/concepts, show a crisp Merit-level improvement, and encourage them in ${langRule}.`;
  } else if (action === 'summarize_bullets') {
    task = `Summarize the selected study passage into crisp ICMA merit-grade bullet-point notes in ${langRule}: Core Idea, 5 Exam Bullets, Key Terms, and Corporate Finance Example.`;
  } else if (action === 'explain_concept') {
    task = `Explain the complex concepts in the selected study passage in ${langRule} with: (1) Simple everyday/Urdu explanation, (2) Real-world Finance/Corporate example, (3) Another angle if still tricky, and (4) Common ICMA exam trap.`;
  } else {
    task = question || `Teach me ${topic} in simple Urdu with a corporate finance example.`;
  }

  const system = `You are a veteran Corporate Finance & Organizational Management Strategist (30 years Finance industry + 50+ years Master Teacher & Recovery Coach). Your student works full-time at an office, starts with ZERO preparation, gets tired after 5:30 PM, and must win the Pakistan Certificate of Merit in ONLY ONE SUBJECT: ICMA M3 Organizational Management (ML-1) on 5 December 2026 (60% MCQs / 40% Typing). ${langRule} Be warm, calm panic/anxiety immediately, and tactically relentless on high-yield past papers and ACCA/CIMA/ICAEW-level mastery.`;

  const userMessage = context
    ? `${task}\n\nContext & Student State:\n${String(context).slice(0, 5500)}`
    : task;

  // 1. Primary: Groq (configured in server env GROQ_API_KEY) — ultra-fast Llama 3.3 70B
  const groqKey = process.env.GROQ_API_KEY;
  if (groqKey) {
    const groqModels = [
      process.env.GROQ_MODEL || 'llama-3.3-70b-versatile',
      'llama-3.1-8b-instant'
    ];
    for (const modelName of groqModels) {
      try {
        const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${groqKey}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            model: modelName,
            messages: [
              { role: 'system', content: system },
              { role: 'user', content: userMessage }
            ],
            temperature: 0.35,
            max_tokens: 1800
          })
        });
        const data = await r.json();
        if (r.ok) {
          const text = data?.choices?.[0]?.message?.content?.trim() || '';
          if (text) {
            return res.json({
              text,
              parsed: tryParseJson(text),
              provider: `Groq (${modelName})`
            });
          }
        }
      } catch {
        // Fall through seamlessly so study is never disrupted
      }
    }
  }

  // 2. Secondary: Google AI Studio Gemini API (process.env.GEMINI_API_KEY)
  const geminiKey = process.env.GEMINI_API_KEY;
  if (geminiKey) {
    const geminiModels = [
      process.env.GEMINI_MODEL || 'gemini-2.5-flash',
      'gemini-2.5-flash-lite'
    ];
    for (const modelName of geminiModels) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
          modelName
        )}:generateContent?key=${encodeURIComponent(geminiKey)}`;
        const r = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: system }] },
            contents: [{ role: 'user', parts: [{ text: userMessage }] }],
            generationConfig: {
              temperature: 0.35,
              maxOutputTokens: 1600
            }
          })
        });
        const data = await r.json();
        if (r.ok) {
          const text =
            data?.candidates?.[0]?.content?.parts
              ?.map((p: any) => p?.text || '')
              .join('')
              .trim() || '';
          if (text) {
            return res.json({
              text,
              parsed: tryParseJson(text),
              provider: `Google Gemini (${modelName})`
            });
          }
        }
      } catch {
        // Fall through
      }
    }
  }

  // 3. Tertiary: OpenRouter free-tier fallback
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
        return res.json({ text, parsed: tryParseJson(text), provider: 'OpenRouter' });
      }
    } catch {
      // Fall through
    }
  }

  // 4. Final Automatic Zero-Key Free Failover: Pollinations Open Text API
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
    return res.json({
      text,
      parsed: tryParseJson(text),
      provider: 'Pollinations Free ($0)'
    });
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
    const firstBrace = raw.indexOf('{');
    const lastBrace = raw.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      try {
        return JSON.parse(raw.slice(firstBrace, lastBrace + 1));
      } catch {
        return null;
      }
    }
    return null;
  }
}