import { AiEngineMode, FreeReferenceItem, McqQuestionItem } from '../types';

interface TutorCallParams {
  action:
    | 'chat'
    | 'mcq'
    | 'flashcards'
    | 'plan'
    | 'numerical'
    | 'typing'
    | 'summarize_bullets'
    | 'explain_concept';
  subject: string;
  topic: string;
  question?: string;
  language?: string;
  count?: number;
  context?: string;
  engineMode?: AiEngineMode;
}

export interface TutorApiResponse {
  text: string;
  parsed?: any;
  provider: string;
}

export async function callFreeAiTutor(params: TutorCallParams): Promise<TutorApiResponse> {
  const {
    action,
    subject,
    topic,
    question = '',
    language = 'bilingual',
    count = 5,
    context = '',
    engineMode = 'auto_free'
  } = params;

  // Try /api/tutor backend first when running on Vercel / server-backed host
  if (engineMode === 'auto_free') {
    try {
      const resp = await fetch('/api/tutor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action,
          subject,
          topic,
          question,
          language,
          count,
          context
        })
      });
      if (resp.ok) {
        const data = await resp.json();
        if (data?.text) {
          return {
            text: String(data.text),
            parsed: data.parsed ?? tryParseJson(String(data.text)),
            provider: data.provider || 'Server Free AI'
          };
        }
      }
    } catch {
      // Static hosts (GitHub Pages / Netlify / local vite preview) fall through cleanly to direct zero-key Pollinations
    }
  }

  return callPollinationsDirect(params);
}

async function callPollinationsDirect(params: TutorCallParams): Promise<TutorApiResponse> {
  const { action, subject, topic, question = '', language = 'bilingual', count = 5, context = '' } = params;
  const langRule =
    language === 'en'
      ? 'Respond in clear English.'
      : 'Respond in clear English with concise Roman Urdu memory cues where helpful.';

  let taskPrompt = question;
  if (action === 'mcq' || action === 'numerical') {
    taskPrompt = `Create ${count} ICMA exam-grade MCQs about "${topic}" (${subject}). Return ONLY valid JSON with no markdown wrapper: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"..."}]}`;
  } else if (action === 'flashcards') {
    taskPrompt = `Create ${count} high-yield active recall flashcards on "${topic}" (${subject}). Return ONLY valid JSON with no markdown wrapper: {"cards":[{"front":"...","back":"...","hint":"..."}]}`;
  } else if (action === 'plan') {
    taskPrompt = `Create a 5-session adaptive study plan for "${topic}" (${subject}). Return ONLY valid JSON with no markdown wrapper: {"sessions":[{"title":"...","minutes":35,"task":"..."}],"strategy":"..."}`;
  } else if (action === 'typing') {
    taskPrompt = `Write 3 short-answer 5-mark ICMA past-paper style questions on "${topic}" with bullet-point model answers.`;
  } else if (action === 'summarize_bullets') {
    taskPrompt = `Summarize this study passage into crisp exam bullet-point notes:\n- **Core Idea** (1 sentence)\n- **Key Bullet Notes** (4-6 bullets)\n- **Key Terms & Exam Hook**`;
  } else if (action === 'explain_concept') {
    taskPrompt = `Explain the complex concepts in this study passage clearly:\n- **Plain-English Meaning**\n- **Step-by-Step Breakdown**\n- **Practical Example**\n- **Exam Watch-Out**`;
  } else if (!taskPrompt) {
    taskPrompt = `Explain "${topic}" (${subject}) clearly with exam structure.`;
  }

  const systemPrompt = `You are ICMAP Smart Tutor for Organizational Management (M3), Accounting, Taxation & Finance. ${langRule} Be concise, accurate, and exam-focused.`;
  const fullUserPrompt = context
    ? `${taskPrompt}\n\nStudy Material Context:\n${context.slice(0, 4500)}`
    : taskPrompt;

  const response = await fetch('https://text.pollinations.ai/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/plain'
    },
    body: JSON.stringify({
      model: 'openai',
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: fullUserPrompt }
      ]
    })
  });

  const raw = (await response.text()).trim();
  if (!response.ok) {
    throw new Error(`Pollinations free AI returned HTTP ${response.status}`);
  }
  if (!raw) {
    throw new Error('Free AI endpoint returned an empty response.');
  }

  let extracted = raw;
  if (raw.startsWith('{')) {
    const obj = tryParseJson(raw);
    const choiceText = obj?.choices?.[0]?.message?.content;
    if (choiceText) extracted = String(choiceText).trim();
  }

  return {
    text: extracted,
    parsed: tryParseJson(extracted),
    provider: 'Pollinations Free ($0 • No Key)'
  };
}

export async function fetchWikipediaConcept(query: string): Promise<FreeReferenceItem> {
  const cleaned = query.trim();
  if (!cleaned) throw new Error('Enter a concept to look up.');

  const searchUrl = `https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(
    cleaned
  )}&srlimit=1&format=json&origin=*`;
  const searchResp = await fetch(searchUrl);
  if (!searchResp.ok) {
    throw new Error(`Wikipedia search returned HTTP ${searchResp.status}`);
  }
  const searchJson = await searchResp.json();
  const hit = searchJson?.query?.search?.[0];
  if (!hit?.title) {
    throw new Error(`No Wikipedia article found for "${cleaned}".`);
  }

  const titleSlug = encodeURIComponent(String(hit.title).replace(/ /g, '_'));
  const summaryResp = await fetch(`https://en.wikipedia.org/api/rest_v1/page/summary/${titleSlug}`);
  if (!summaryResp.ok) {
    throw new Error(`Wikipedia summary returned HTTP ${summaryResp.status}`);
  }
  const summaryJson = await summaryResp.json();
  const extract = String(summaryJson?.extract || '').trim();
  if (!extract) {
    throw new Error('Wikipedia summary was empty.');
  }

  return {
    sourceName: 'Wikipedia REST API ($0 • No Key)',
    title: String(summaryJson.title || hit.title),
    subtitle: String(summaryJson.description || 'Encyclopedia Summary'),
    summary: extract,
    url: String(summaryJson?.content_urls?.desktop?.page || `https://en.wikipedia.org/wiki/${titleSlug}`)
  };
}

export async function fetchDictionaryWord(wordInput: string): Promise<FreeReferenceItem> {
  const cleaned = wordInput.trim().split(/\s+/)[0]?.toLowerCase() || '';
  if (!cleaned) throw new Error('Enter a single word to look up.');

  const resp = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(cleaned)}`);
  if (!resp.ok) {
    throw new Error(
      resp.status === 404
        ? `No dictionary entry found for "${cleaned}". Try Wikipedia Concept lookup.`
        : `Dictionary API returned HTTP ${resp.status}`
    );
  }

  const arr = await resp.json();
  const entry = Array.isArray(arr) ? arr[0] : null;
  if (!entry) throw new Error('Empty dictionary response.');

  const meanings = Array.isArray(entry.meanings) ? entry.meanings.slice(0, 3) : [];
  const lines: string[] = [];
  for (const m of meanings) {
    const def = m?.definitions?.[0];
    if (def?.definition) {
      lines.push(`[${m.partOfSpeech || 'term'}] ${def.definition}`);
      if (def.example) {
        lines.push(`Example: "${def.example}"`);
      }
    }
  }

  if (!lines.length) throw new Error(`No definitions available for "${cleaned}".`);

  return {
    sourceName: 'Free Dictionary API ($0 • No Key)',
    title: String(entry.word || cleaned).replace(/^./, (c) => c.toUpperCase()),
    subtitle: String(entry.phonetic || 'English Lexicon'),
    summary: lines.join('\n'),
    url: `https://en.wiktionary.org/wiki/${encodeURIComponent(cleaned)}`
  };
}

export function parseMcqArrayFromAi(parsed: any, bankId = 'ai_generated'): McqQuestionItem[] {
  const rawList = Array.isArray(parsed?.questions) ? parsed.questions : [];
  return rawList
    .filter((q: any) => q?.question && Array.isArray(q?.options) && q.options.length >= 2)
    .map((q: any, idx: number) => ({
      id: `ai_mcq_${Date.now()}_${idx}`,
      bankId,
      question: String(q.question).trim(),
      options: q.options.map((opt: any) => String(opt).trim()),
      answer: Math.max(0, Math.min(q.options.length - 1, Number(q.answer ?? 0) || 0)),
      explanation: String(q.explanation || 'Review the underlying concept in your study text.').trim()
    }));
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
