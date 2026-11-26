import {
  AiEngineMode,
  FreeReferenceItem,
  McqQuestionItem,
  TypingQuestionItem
} from '../types';

export type TutorActionType =
  | 'chat'
  | 'mcq'
  | 'flashcards'
  | 'plan'
  | 'daily_brief'
  | 'numerical'
  | 'typing'
  | 'grade_typing'
  | 'summarize_bullets'
  | 'explain_concept';

interface TutorCallParams {
  action: TutorActionType;
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
    language = 'ur',
    count = 5,
    context = '',
    engineMode = 'auto_free'
  } = params;

  // Primary serverless Vercel route (/api/tutor checks process.env.GROQ_API_KEY first, then Gemini, OpenRouter, Pollinations)
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
          language: language === 'en' ? 'english' : 'urdu_bilingual',
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
            provider: data.provider || 'Groq / Free AI Backend'
          };
        }
      }
    } catch {
      // Static hosts fallback cleanly to zero-key Pollinations
    }
  }

  return callPollinationsDirect(params);
}

async function callPollinationsDirect(params: TutorCallParams): Promise<TutorApiResponse> {
  const { action, subject, topic, question = '', language = 'ur', count = 5, context = '' } = params;
  const langRule =
    language === 'en'
      ? 'Respond in crisp, authoritative English with practical corporate examples.'
      : 'Respond in warm, easy-to-speak Roman Urdu / Urdu mixed with core ICMA English exam keywords so an executive finance student understands effortlessly even when tired.';

  let taskPrompt = question;
  if (action === 'mcq' || action === 'numerical') {
    taskPrompt = `Create ${count} merit-grade ICMA/ACCA/CIMA/ICAEW MCQs on "${topic}" (${subject}). Return ONLY valid JSON with no markdown wrapper: {"questions":[{"question":"...","options":["A","B","C","D"],"answer":0,"explanation":"...","topicTag":"${topic}","sourceBenchmark":"ICMA/ACCA/CIMA"}]}`;
  } else if (action === 'flashcards') {
    taskPrompt = `Create ${count} high-yield spaced-repetition flashcards on "${topic}" (${subject}). Return ONLY valid JSON with no markdown wrapper: {"cards":[{"front":"...","back":"...","hint":"..."}]}`;
  } else if (action === 'plan' || action === 'daily_brief') {
    taskPrompt = `Deliver today's Strategist & Recovery Briefing for a busy Finance office student aiming to win the Pakistan Certificate of Merit in ICMA OM on 5 December 2026. Cover total study hours, 08:00 AM, 12:45–13:15 Office Break + Short Test, 15:30 Break 2, 17:30 tired 5 flashcards, 19:00–20:00 fresh block, 15m recovery break, 20:15 Daily 60/40 Test, 23:30 sleep, plus upcoming Mock alerts.`;
  } else if (action === 'typing') {
    taskPrompt = `Create ${count} 5-mark ICMA ML-1 / CIMA / ACCA typing questions on "${topic}". Return ONLY valid JSON with no markdown wrapper: {"questions":[{"question":"...","marks":5,"topicTag":"${topic}","keywords":["..."],"modelAnswer":"..."}]}`;
  } else if (action === 'grade_typing') {
    taskPrompt = `Grade the student's 5-mark typed answer strictly against ICMA examiner marking standards. Give marks/5, missing bullet points, and a 3-line Certificate of Merit improvement in ${langRule}.`;
  } else if (action === 'summarize_bullets') {
    taskPrompt = `Summarize this passage into ICMA merit-grade bullet notes (${langRule}): Core Idea, 5 Exam Bullets, Keywords, and Corporate Example.`;
  } else if (action === 'explain_concept') {
    taskPrompt = `Explain this passage in ${langRule}: (1) Simple explanation, (2) Real Corporate Finance example, (3) Alternative simple angle, and (4) Exam Trap.`;
  } else if (!taskPrompt) {
    taskPrompt = `Teach "${topic}" (${subject}) in ${langRule} with a practical office/corporate example.`;
  }

  const systemPrompt = `You are a veteran Corporate Finance & OM Strategist (30 yrs industry + 50+ yrs Master Teacher & Recovery Coach) helping a busy executive student top Pakistan in ICMA M3 Organizational Management on 5 December 2026 from Zero Prep. ${langRule}`;
  const fullUserPrompt = context
    ? `${taskPrompt}\n\nStudy Context:\n${context.slice(0, 4500)}`
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
    provider: 'Pollinations Free ($0 Failover)'
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
    sourceName: 'Wikipedia REST API ($0 • Live Web Lookup)',
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

export function parseMcqArrayFromAi(
  parsed: any,
  bankId = 'ai_generated',
  defaultTopic = 'General OM'
): McqQuestionItem[] {
  const rawList = Array.isArray(parsed?.questions) ? parsed.questions : [];
  return rawList
    .filter((q: any) => q?.question && Array.isArray(q?.options) && q.options.length >= 2)
    .map((q: any, idx: number) => ({
      id: `ai_mcq_${Date.now()}_${idx}`,
      bankId,
      topicTag: String(q.topicTag || defaultTopic).trim(),
      sourceBenchmark: String(q.sourceBenchmark || 'AI Exam Bank').trim(),
      question: String(q.question).trim(),
      options: q.options.map((opt: any) => String(opt).trim()),
      answer: Math.max(0, Math.min(q.options.length - 1, Number(q.answer ?? 0) || 0)),
      explanation: String(q.explanation || 'Review the underlying concept in your study text.').trim()
    }));
}

export function parseTypingQuestionsFromAi(
  parsed: any,
  defaultTopic = 'General OM'
): TypingQuestionItem[] {
  const rawList = Array.isArray(parsed?.questions) ? parsed.questions : [];
  return rawList
    .filter((q: any) => q?.question && q?.modelAnswer)
    .map((q: any, idx: number) => ({
      id: `ai_typ_${Date.now()}_${idx}`,
      topicTag: String(q.topicTag || defaultTopic).trim(),
      marks: Number(q.marks ?? 5) || 5,
      sourceBenchmark: String(q.sourceBenchmark || 'ICMA/ACCA/CIMA 5-Mark').trim(),
      question: String(q.question).trim(),
      keywords: Array.isArray(q.keywords)
        ? q.keywords.map((k: any) => String(k).trim()).filter(Boolean)
        : [],
      modelAnswer: String(q.modelAnswer).trim()
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
