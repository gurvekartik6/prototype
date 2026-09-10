import { useRef, useState } from 'react';

export function useSpeech(lang = 'en-IN') {
  const [recording, setRecording] = useState(false);

  const rec = useRef<any>(null);

  const start = (onText: (text: string) => void) => {
    const SR =
      (window as any).SpeechRecognition ||
      (window as any).webkitSpeechRecognition;

    if (!SR) {
      return false;
    }

    const r = new SR();

    r.lang = lang;
    r.continuous = true;
    r.interimResults = false;

    r.onresult = (e: any) => {
      let s = '';

      for (
        let i = e.resultIndex;
        i < e.results.length;
        i++
      ) {
        s += e.results[i][0].transcript + ' ';
      }

      onText(s);
    };

    r.onend = () => {
      setRecording(false);
    };

    r.start();

    rec.current = r;
    setRecording(true);

    return true;
  };

  const stop = () => {
    rec.current?.stop();
    setRecording(false);
  };

  const speak = (text: string) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.speak(
        new SpeechSynthesisUtterance(text)
      );
    }
  };

  return {
    recording,
    start,
    stop,
    speak,
  };
}