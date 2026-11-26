import { TutorLang } from '../types';

const FEMALE_VOICE_PATTERNS = [
  'female',
  'woman',
  'girl',
  'lady',
  'uzma',
  'asma',
  'sana',
  'ayesha',
  'fatima',
  'zainab',
  'samantha',
  'victoria',
  'karen',
  'moira',
  'tessa',
  'zira',
  'hazel',
  'susan',
  'heera',
  'kalpana',
  'veena',
  'lekha',
  'fiona',
  'serena',
  'catherine',
  'allison',
  'ava',
  'joanna',
  'salli',
  'kendra',
  'kimberly',
  'ivy',
  'aria',
  'jenny',
  'sonia',
  'libby',
  'mia',
  'neerja',
  'swara',
  'aditi',
  'raveena',
  'kajal',
  'urf',
  'ur-pk-x-urf',
  'en-us-x-sfg',
  'en-us-x-tpf',
  'en-gb-x-gba',
  'en-gb-x-gbc',
  'hi-in-x-hia',
  'hi-in-x-hie',
  'google us english',
  'google uk english female'
];

const MALE_VOICE_PATTERNS = [
  'male',
  'man',
  'asad',
  'salman',
  'bilal',
  'hamza',
  'usman',
  'tariq',
  'david',
  'mark',
  'george',
  'daniel',
  'ryan',
  'guy',
  'christopher',
  'eric',
  'andrew',
  'brian',
  'alex',
  'fred',
  'rishi',
  'prabhat',
  'madhur',
  'matthew',
  'joey',
  'justin',
  'arthur',
  'oliver',
  'thomas',
  'james',
  'aaron',
  'urm',
  'ur-pk-x-urm',
  'ur-in-x-urm',
  'en-us-x-iom',
  'en-us-x-tpd',
  'en-gb-x-rjs',
  'en-gb-x-gbd',
  'hi-in-x-hid',
  'google uk english male'
];

export function isExplicitFemaleVoice(voice: SpeechSynthesisVoice): boolean {
  const sig = `${voice.name} ${voice.voiceURI}`.toLowerCase();
  return FEMALE_VOICE_PATTERNS.some((pattern) => sig.includes(pattern));
}

export function isExplicitMaleVoice(voice: SpeechSynthesisVoice): boolean {
  const sig = `${voice.name} ${voice.voiceURI}`.toLowerCase();
  return MALE_VOICE_PATTERNS.some((pattern) => sig.includes(pattern));
}

export function pickMaleTutorVoice(
  voices: SpeechSynthesisVoice[],
  targetLang: TutorLang
): { voice: SpeechSynthesisVoice | null; isVerifiedMale: boolean } {
  if (!voices.length) return { voice: null, isVerifiedMale: false };

  // Strip out every known female voice first so a female voice is never chosen when male/neutral exists
  const nonFemaleVoices = voices.filter((v) => !isExplicitFemaleVoice(v));
  const pool = nonFemaleVoices.length > 0 ? nonFemaleVoices : [];
  if (!pool.length) return { voice: null, isVerifiedMale: false };

  const scoreVoice = (v: SpeechSynthesisVoice): number => {
    const langLower = v.lang.toLowerCase();
    const isMale = isExplicitMaleVoice(v);
    let score = 0;

    if (isMale) score += 200;

    if (targetLang === 'ur') {
      if (langLower.startsWith('ur-pk') || langLower === 'ur') score += 90;
      else if (langLower.startsWith('hi-in')) score += 60;
      else if (langLower.startsWith('en-pk') || langLower.startsWith('en-in')) score += 50;
      else if (langLower.startsWith('ar')) score += 35;
      else if (langLower.startsWith('en')) score += 25;
    } else {
      if (langLower.startsWith('en-gb') || langLower.startsWith('en-us')) score += 80;
      else if (langLower.startsWith('en')) score += 65;
    }

    if (v.localService) score += 10;
    return score;
  };

  const sorted = [...pool].sort((a, b) => scoreVoice(b) - scoreVoice(a));
  const best = sorted[0] || null;
  return {
    voice: best,
    isVerifiedMale: best ? isExplicitMaleVoice(best) : false
  };
}

let cachedVoices: SpeechSynthesisVoice[] = [];
let voicesListenerAttached = false;

export function preloadMaleVoices(): SpeechSynthesisVoice[] {
  if (typeof window === 'undefined' || !window.speechSynthesis) return [];
  cachedVoices = window.speechSynthesis.getVoices() || [];
  if (!voicesListenerAttached) {
    voicesListenerAttached = true;
    const refresh = () => {
      if (typeof window !== 'undefined' && window.speechSynthesis) {
        cachedVoices = window.speechSynthesis.getVoices() || [];
      }
    };
    if (typeof window.speechSynthesis.addEventListener === 'function') {
      window.speechSynthesis.addEventListener('voiceschanged', refresh);
    } else {
      window.speechSynthesis.onvoiceschanged = refresh;
    }
  }
  return cachedVoices;
}

export function getActiveMaleVoiceLabel(targetLang: TutorLang): string {
  const voices = cachedVoices.length ? cachedVoices : preloadMaleVoices();
  const { voice, isVerifiedMale } = pickMaleTutorVoice(voices, targetLang);
  if (voice && isVerifiedMale) return `Male Voice (${voice.name.split(' ')[0]})`;
  if (voice) return `Male Tutor Register (${voice.lang})`;
  return 'Male Mentor Voice';
}

export function speakWithMaleTutor(
  text: string,
  targetLang: TutorLang,
  callbacks?: {
    onStart?: () => void;
    onEnd?: () => void;
    onError?: () => void;
  }
) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();

  const clean = String(text)
    .replace(/[#*`]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 1800);
  if (!clean) return;

  const voices = preloadMaleVoices();
  const { voice, isVerifiedMale } = pickMaleTutorVoice(voices, targetLang);

  const utterance = new SpeechSynthesisUtterance(clean);
  if (voice) {
    utterance.voice = voice;
    utterance.lang = voice.lang || (targetLang === 'ur' ? 'ur-PK' : 'en-US');
    // Authoritative low male mentor pitch when neutral/fallback voice is used, subtle warmth for verified male voices
    utterance.pitch = isVerifiedMale ? 0.85 : 0.68;
  } else {
    utterance.lang = targetLang === 'ur' ? 'ur-PK' : 'en-US';
    utterance.pitch = 0.68;
  }
  utterance.rate = 0.94;
  utterance.volume = 1;

  utterance.onstart = () => callbacks?.onStart?.();
  utterance.onend = () => callbacks?.onEnd?.();
  utterance.onerror = () => callbacks?.onError?.();

  callbacks?.onStart?.();
  window.speechSynthesis.speak(utterance);
}

export function stopTutorSpeech() {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
}
