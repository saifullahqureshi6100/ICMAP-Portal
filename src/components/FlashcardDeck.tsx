import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  HelpCircle,
  Maximize2,
  Minimize2,
  Plus,
  RotateCcw,
  Shuffle,
  Sparkles,
  Trash2
} from 'lucide-react';
import { STUDY_MATERIALS } from '../data/icmapData';
import { FlashcardItem } from '../types';

interface FlashcardDeckProps {
  cards: FlashcardItem[];
  todayFiveCardIds: string[];
  defaultTodayFiveMode?: boolean;
  onRecordReview: (cardId: string, mastered: boolean) => void;
  onAddCustomCard: (deck: string, front: string, back: string, hint: string) => void;
  onDeleteCard: (cardId: string) => void;
  onGenerateFromMaterial: (materialId: string, count: number) => Promise<void>;
  isGenerating: boolean;
  isFullScreen: boolean;
  onToggleFullScreen: () => void;
}

export const FlashcardDeck: React.FC<FlashcardDeckProps> = ({
  cards,
  todayFiveCardIds,
  defaultTodayFiveMode = false,
  onRecordReview,
  onAddCustomCard,
  onDeleteCard,
  onGenerateFromMaterial,
  isGenerating,
  isFullScreen,
  onToggleFullScreen
}) => {
  const decks = useMemo(
    () => [
      "Today's 5 Daily Cards (5:30 PM)",
      'All Decks',
      ...Array.from(new Set(cards.map((c) => c.deck)))
    ],
    [cards]
  );

  const [selectedDeck, setSelectedDeck] = useState(
    defaultTodayFiveMode ? "Today's 5 Daily Cards (5:30 PM)" : 'All Decks'
  );
  const [shuffledOrder, setShuffledOrder] = useState<string[]>([]);
  const [index, setIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [showHint, setShowHint] = useState(false);

  const [genMaterialId, setGenMaterialId] = useState(STUDY_MATERIALS[0].id);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newFront, setNewFront] = useState('');
  const [newBack, setNewBack] = useState('');
  const [newHint, setNewHint] = useState('');

  useEffect(() => {
    if (defaultTodayFiveMode) {
      setSelectedDeck("Today's 5 Daily Cards (5:30 PM)");
      setIndex(0);
    }
  }, [defaultTodayFiveMode]);

  const filteredCards = useMemo(() => {
    const base =
      selectedDeck === "Today's 5 Daily Cards (5:30 PM)"
        ? cards.filter((c) => todayFiveCardIds.includes(c.id))
        : selectedDeck === 'All Decks'
          ? cards
          : cards.filter((c) => c.deck === selectedDeck);
    if (!shuffledOrder.length) return base;
    const rank = new Map(shuffledOrder.map((id, idx) => [id, idx]));
    return [...base].sort((a, b) => (rank.get(a.id) ?? 9999) - (rank.get(b.id) ?? 9999));
  }, [cards, selectedDeck, todayFiveCardIds, shuffledOrder]);

  useEffect(() => {
    if (index >= filteredCards.length && filteredCards.length > 0) {
      setIndex(0);
    }
  }, [filteredCards.length, index]);

  const currentCard = filteredCards[index] ?? null;

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const target = e.target as HTMLElement | null;
      if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return;
      if (!filteredCards.length) return;
      if (e.code === 'Space') {
        e.preventDefault();
        setIsFlipped((f) => !f);
      } else if (e.key === 'ArrowRight') {
        setIndex((i) => (i + 1) % filteredCards.length);
        setIsFlipped(false);
        setShowHint(false);
      } else if (e.key === 'ArrowLeft') {
        setIndex((i) => (i - 1 + filteredCards.length) % filteredCards.length);
        setIsFlipped(false);
        setShowHint(false);
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [filteredCards.length]);

  function handleShuffle() {
    const ids = filteredCards.map((c) => c.id);
    for (let i = ids.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [ids[i], ids[j]] = [ids[j], ids[i]];
    }
    setShuffledOrder(ids);
    setIndex(0);
    setIsFlipped(false);
    setShowHint(false);
  }

  function stepCard(delta: number) {
    if (!filteredCards.length) return;
    setIndex((i) => (i + delta + filteredCards.length) % filteredCards.length);
    setIsFlipped(false);
    setShowHint(false);
  }

  function handleGrade(mastered: boolean) {
    if (!currentCard) return;
    onRecordReview(currentCard.id, mastered);
    stepCard(1);
  }

  function submitCustomCard(e: React.FormEvent) {
    e.preventDefault();
    if (!newFront.trim() || !newBack.trim()) return;
    const targetDeck =
      selectedDeck === 'All Decks' || selectedDeck.startsWith("Today's 5")
        ? 'Custom Flashcards'
        : selectedDeck;
    onAddCustomCard(targetDeck, newFront.trim(), newBack.trim(), newHint.trim());
    setNewFront('');
    setNewBack('');
    setNewHint('');
    setShowAddForm(false);
  }

  const masteredCount = filteredCards.filter((c) => c.mastered).length;
  const dayBadge = (offset?: number) => {
    if (offset === -1) return 'Yesterday Review';
    if (offset === -2) return '2–3 Days Ago Review';
    if (offset === 1) return 'Upcoming Topic Prep';
    return 'Today Focus';
  };

  return (
    <div className="space-y-5">
      <div className="glass rounded-3xl p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-widest text-cyan-300 font-semibold flex items-center gap-1.5">
              <Clock size={13} /> Post-Office 5:30 PM Tired Recovery & Spaced Repetition
            </div>
            <h2 className="text-xl md:text-2xl font-bold mt-0.5">
              Shuffle & Flip Deck ({masteredCount}/{filteredCards.length} Mastered)
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Curated daily mix of yesterday, 2–3 days back, weak test topics & upcoming concepts • Tap card or press{' '}
              <kbd className="px-1.5 py-0.5 rounded bg-white/10">Space</kbd> to flip
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleShuffle}
              className="px-3.5 py-2.5 rounded-xl bg-violet-500/20 hover:bg-violet-500/30 text-violet-100 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
            >
              <Shuffle size={15} /> Shuffle Deck
            </button>
            <button
              type="button"
              onClick={() => setShowAddForm((v) => !v)}
              className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs flex items-center gap-1.5 min-h-[44px]"
            >
              <Plus size={15} /> Add Card
            </button>
            <button
              type="button"
              onClick={onToggleFullScreen}
              aria-label={isFullScreen ? 'Exit full-screen flashcards' : 'Full-screen flashcards'}
              className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 min-w-[44px] min-h-[44px] flex items-center justify-center"
            >
              {isFullScreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
            </button>
          </div>
        </div>

        {/* AI Generate Today's 5 Cards from Reading Material */}
        <div className="mt-4 pt-4 border-t border-white/10 flex flex-col sm:flex-row gap-2.5 items-stretch sm:items-center">
          <select
            value={genMaterialId}
            onChange={(e) => setGenMaterialId(e.target.value)}
            className="flex-1 bg-black/30 border border-white/10 rounded-xl px-3 py-2.5 text-xs text-slate-200 min-h-[44px]"
          >
            {STUDY_MATERIALS.map((m) => (
              <option key={m.id} value={m.id} className="bg-[#07111f]">
                {m.title}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={isGenerating}
            onClick={() => onGenerateFromMaterial(genMaterialId, 5)}
            className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-violet-500 text-xs font-semibold flex items-center justify-center gap-1.5 min-h-[44px]"
          >
            <Sparkles size={15} />
            {isGenerating ? 'Generating 5 AI Cards...' : 'Generate 5 Today Cards from Reading'}
          </button>
        </div>

        {/* Deck Filter Pills */}
        <div className="flex gap-2 overflow-x-auto pb-1 mt-4 scrollbar">
          {decks.map((deck) => (
            <button
              key={deck}
              type="button"
              onClick={() => {
                setSelectedDeck(deck);
                setIndex(0);
                setIsFlipped(false);
                setShowHint(false);
              }}
              className={`px-3 py-1.5 rounded-full text-xs whitespace-nowrap border transition ${
                selectedDeck === deck
                  ? 'bg-cyan-400/20 text-cyan-200 border-cyan-300/40 font-semibold'
                  : 'bg-white/5 text-slate-400 border-white/10 hover:text-white'
              }`}
            >
              {deck}
            </button>
          ))}
        </div>
      </div>

      {/* Custom Flashcard Form */}
      {showAddForm && (
        <form onSubmit={submitCustomCard} className="glass rounded-3xl p-5 space-y-3">
          <div className="text-sm font-bold text-cyan-200">Create Custom Flashcard</div>
          <input
            value={newFront}
            onChange={(e) => setNewFront(e.target.value)}
            placeholder="Front (Question, concept, or formula prompt)"
            className="w-full bg-black/30 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm"
          />
          <textarea
            value={newBack}
            onChange={(e) => setNewBack(e.target.value)}
            rows={3}
            placeholder="Back (Model answer, steps, or definition)"
            className="w-full bg-black/30 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm"
          />
          <input
            value={newHint}
            onChange={(e) => setNewHint(e.target.value)}
            placeholder="Optional memory hint"
            className="w-full bg-black/30 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm"
          />
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="px-4 py-2 rounded-xl bg-white/5 text-xs"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-cyan-500/25 text-cyan-100 text-xs font-semibold"
            >
              Save Card
            </button>
          </div>
        </form>
      )}

      {/* 3D Flip Card Stage */}
      {currentCard ? (
        <div className="glass rounded-3xl p-5 md:p-7">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400 mb-4">
            <span>
              Card {index + 1} of {filteredCards.length} • {currentCard.topicTag || currentCard.deck} •{' '}
              <span className="text-violet-300">{dayBadge(currentCard.dayOffsetTag)}</span>
            </span>
            <div className="flex items-center gap-2">
              {currentCard.mastered && (
                <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-200 font-semibold">
                  Mastered
                </span>
              )}
              <button
                type="button"
                onClick={() => onDeleteCard(currentCard.id)}
                aria-label="Delete flashcard"
                className="p-1.5 rounded-lg bg-white/5 hover:bg-red-500/20 text-slate-400 hover:text-red-200"
              >
                <Trash2 size={14} />
              </button>
            </div>
          </div>

          <div className="perspective-1200 w-full max-w-2xl mx-auto">
            <button
              type="button"
              onClick={() => setIsFlipped((f) => !f)}
              className="relative w-full min-h-[260px] md:min-h-[300px] text-left focus:outline-none"
            >
              <div
                className={`relative w-full min-h-[260px] md:min-h-[300px] transition-transform duration-500 preserve-3d ${
                  isFlipped ? 'rotate-y-180' : ''
                }`}
              >
                <div className="absolute inset-0 backface-hidden rounded-3xl bg-gradient-to-br from-cyan-500/18 via-slate-900/90 to-violet-500/18 border border-cyan-300/25 p-6 md:p-8 flex flex-col justify-between">
                  <div className="flex items-center justify-between text-[11px] uppercase tracking-widest text-cyan-300 font-semibold">
                    <span>Question / Recall Prompt</span>
                    <span>Tap or Space to Flip</span>
                  </div>
                  <div className="text-lg md:text-2xl font-bold leading-snug text-center my-auto px-2">
                    {currentCard.front}
                  </div>
                  <div className="text-center text-xs text-slate-400">
                    Reviews: {currentCard.reviewCount}
                  </div>
                </div>

                <div className="absolute inset-0 backface-hidden rotate-y-180 rounded-3xl bg-gradient-to-br from-emerald-500/16 via-slate-900/95 to-cyan-500/18 border border-emerald-300/30 p-6 md:p-8 flex flex-col justify-between">
                  <div className="flex items-center justify-between text-[11px] uppercase tracking-widest text-emerald-300 font-semibold">
                    <span>Model Answer</span>
                    <span>Tap to Flip Back</span>
                  </div>
                  <div className="text-sm md:text-base leading-relaxed whitespace-pre-wrap text-slate-100 my-auto">
                    {currentCard.back}
                  </div>
                  <div className="text-center text-xs text-slate-400">
                    Self-grade below to update mastery
                  </div>
                </div>
              </div>
            </button>
          </div>

          {currentCard.hint && (
            <div className="max-w-2xl mx-auto mt-3 text-center">
              {showHint ? (
                <div className="text-xs text-amber-200 bg-amber-400/10 border border-amber-300/20 rounded-xl px-3.5 py-2">
                  Hint: {currentCard.hint}
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setShowHint(true)}
                  className="text-xs text-slate-400 hover:text-cyan-200 inline-flex items-center gap-1"
                >
                  <HelpCircle size={13} /> Show Memory Hint
                </button>
              )}
            </div>
          )}

          <div className="max-w-2xl mx-auto mt-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => stepCard(-1)}
                className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs flex items-center gap-1 min-h-[44px]"
              >
                <ChevronLeft size={16} /> Prev
              </button>
              <button
                type="button"
                onClick={() => stepCard(1)}
                className="px-3.5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs flex items-center gap-1 min-h-[44px]"
              >
                Next <ChevronRight size={16} />
              </button>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleGrade(false)}
                className="px-3.5 py-2.5 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-200 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
              >
                <RotateCcw size={14} /> Review Again
              </button>
              <button
                type="button"
                onClick={() => handleGrade(true)}
                className="px-4 py-2.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-200 text-xs font-semibold flex items-center gap-1.5 min-h-[44px]"
              >
                <CheckCircle2 size={14} /> Got It
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="glass rounded-3xl p-10 text-center text-slate-400 text-sm">
          No flashcards in this filter yet. Switch to "All Decks" or generate 5 cards above!
        </div>
      )}
    </div>
  );
};
