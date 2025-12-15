import { useState, useRef, useEffect } from 'react'
import Auth from './Auth' // Import the new Auth component
import './App.css'

// 1. Centralized Configuration
const API_BASE = "http://localhost:8080/api";

function App() {
  // --- AUTH STATE ---
  const [user, setUser] = useState(null);

  // Check for existing login on load
  useEffect(() => {
    const token = localStorage.getItem("token");
    const savedUser = localStorage.getItem("user");
    if (token && savedUser) {
      setUser(JSON.parse(savedUser));
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
    setMode("menu");
    setFile(null);
    setTopics([]);
  };

  // --- APP STATE ---
  const [file, setFile] = useState(null)
  const [status, setStatus] = useState("idle") // idle, uploading, analyzing, ready, error
  const [errorMessage, setErrorMessage] = useState("")
  const [topics, setTopics] = useState([])
  const [fileName, setFileName] = useState("")
  
  // Chat & Voice State
  const [selectedTopic, setSelectedTopic] = useState(null)
  const [difficulty, setDifficulty] = useState("medium")
  const [explanation, setExplanation] = useState("")
  const [feedback, setFeedback] = useState("")
  
  // Loading & Activity States
  const [isGrading, setIsGrading] = useState(false)
  const [isListening, setIsListening] = useState(false)
  const [isSpeaking, setIsSpeaking] = useState(false)

  // Navigation State
  const [mode, setMode] = useState("menu") 

  // Refs
  const recognitionRef = useRef(null);
  const abortControllerRef = useRef(null);

  // --- HELPER: TEXT TO SPEECH ---
  const stopSpeaking = () => {
    window.speechSynthesis.cancel();
    setIsSpeaking(false);
  };

  const speakFeedback = (text) => {
    stopSpeaking();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1; 
    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);
    window.speechSynthesis.speak(utterance);
  };

  // --- HELPER: SPEECH TO TEXT ---
  const toggleListening = () => {
    if (isListening) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
        setIsListening(false);
      }
      return;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) { alert("Browser does not support voice."); return; }
    
    const recognition = new SpeechRecognition();
    recognition.lang = 'en-US';
    recognition.continuous = false;
    
    recognitionRef.current = recognition; 

    recognition.onstart = () => setIsListening(true);
    
    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      setExplanation(prev => (prev ? prev + " " + transcript : transcript));
      setIsListening(false);
    };

    recognition.onerror = (event) => {
      console.error("Speech error", event);
      setIsListening(false);
    };
    
    recognition.onend = () => setIsListening(false);
    recognition.start();
  };

  // --- API HANDLERS ---

  const handleUpload = async () => {
    if (!file) return;
    setErrorMessage("");
    setStatus("uploading");

    try {
      const token = localStorage.getItem("token");
      const formData = new FormData();
      formData.append("file", file);

      const uploadRes = await fetch(`${API_BASE}/documents/upload`, { 
        method: "POST", 
        headers: { "Authorization": `Bearer ${token}` }, 
        body: formData 
      });

      // --- AUTO-LOGOUT CHECK ---
      if (uploadRes.status === 403) {
        handleLogout();
        alert("Session expired. Please log in again.");
        return;
      }

      if (!uploadRes.ok) throw new Error(`Upload failed: ${uploadRes.statusText}`);
      const uploadData = await uploadRes.json();
      setFileName(uploadData.fileName);

      setStatus("analyzing")
      const analyzeRes = await fetch(`${API_BASE}/study/analyze`, {
        method: "POST",
        headers: { 
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ fileName: uploadData.fileName })
      });

      if (!analyzeRes.ok) throw new Error("Analysis failed");
      const topicsData = await analyzeRes.json();
      
      setTopics(topicsData);
      setStatus("ready");
      setMode("menu");

    } catch (error) {
      console.error(error);
      setStatus("idle");  
      setErrorMessage(error.message);
    }
  };

  const handleSubmitExplanation = async () => {
    if (!explanation) return;
    setIsGrading(true);
    setErrorMessage("");
    stopSpeaking();
    
    try {
        const token = localStorage.getItem("token");
        const res = await fetch(`${API_BASE}/study/feynman-check`, {
          method: "POST",
          headers: { 
              "Content-Type": "application/json",
              "Authorization": `Bearer ${token}` 
          },
          body: JSON.stringify({ 
            concept: selectedTopic, 
            explanation: explanation,
            difficulty: difficulty 
          })
        });

        // --- AUTO-LOGOUT CHECK ---
        if (res.status === 403) {
            handleLogout();
            return;
        }
        
        if (!res.ok) throw new Error("Grading failed");
        
        const text = await res.text();
        setFeedback(text);
        speakFeedback(text); 

    } catch (error) {
        console.error(error);
        setFeedback("Error: Could not grade explanation.");
    } finally {
        setIsGrading(false);
    }
  };

  const handleToggleAnalogy = async () => {
    if (isSpeaking || isGrading) {
      stopSpeaking();
      if (abortControllerRef.current) abortControllerRef.current.abort();
      setIsGrading(false);
      return;
    }

    setIsGrading(true);
    setErrorMessage("");
    abortControllerRef.current = new AbortController();

    try {
        const token = localStorage.getItem("token");
        const res = await fetch(`${API_BASE}/study/analogy`, {
          method: "POST",
          headers: { 
              "Content-Type": "application/json",
              "Authorization": `Bearer ${token}`
          },
          body: JSON.stringify({ concept: selectedTopic, difficulty: difficulty }),
          signal: abortControllerRef.current.signal
        });

        // --- AUTO-LOGOUT CHECK ---
        if (res.status === 403) {
            handleLogout();
            return;
        }

        if (!res.ok) throw new Error("Analogy generation failed");

        const text = await res.text();
        setFeedback("💡 ANALOGY: " + text);
        speakFeedback(text);
    } catch (error) {
        if (error.name !== 'AbortError') {
          console.error(error);
          setFeedback("Error generating analogy.");
        }
    } finally {
        setIsGrading(false);
    }
  };

  const startChat = (topic, level) => {
    setSelectedTopic(topic);
    setDifficulty(level);
    setMode("chat");
    setExplanation("");
    setFeedback("");
    stopSpeaking();
  };

  // --- RENDER (CONDITIONAL) ---
  
  // If not logged in, show Auth Screen
  if (!user) {
    return <Auth onLogin={(userData) => setUser(userData)} />;
  }

  // If logged in, show Main App
  return (
    <div className="container">
      <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
        <h1>Feynmind 🧠</h1>
        <button onClick={handleLogout} style={{background: '#333', fontSize:'0.8em', padding: '5px 10px'}}>
           Logout ({user.name})
        </button>
      </div>
      
      {errorMessage && (
        <div style={{background: '#ffdddd', color: 'red', padding: '10px', borderRadius: '5px', marginBottom: '15px', border: '1px solid red'}}>
            ⚠️ {errorMessage}
        </div>
      )}

      {/* UPLOAD */}
      {status !== 'ready' && (
        <div className="card">
          <input type="file" accept=".pdf" onChange={(e) => setFile(e.target.files[0])} />
          <button onClick={handleUpload} disabled={status === 'uploading' || status === 'analyzing'}>
            {status === 'idle' ? "Upload & Start" : status === 'uploading' ? "Reading PDF..." : "Extracting Concepts..."}
          </button>
        </div>
      )}

      {/* MENU */}
      {status === 'ready' && mode === 'menu' && (
        <div className="card">
          <h2>Select a Topic</h2>
          <div className="topic-list">
            {topics.map((t, i) => (
              <div key={i} className="topic-item" style={{marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '8px'}}>
                <h3>📚 {t}</h3>
                <div style={{marginTop:'10px', display: 'flex', gap: '10px', justifyContent: 'center'}}>
                  <button className="difficulty-btn easy" style={{background: '#4caf50'}} onClick={() => startChat(t, 'easy')}>Easy</button>
                  <button className="difficulty-btn medium" style={{background: '#2196f3'}} onClick={() => startChat(t, 'medium')}>Medium</button>
                  <button className="difficulty-btn hard" style={{background: '#f44336'}} onClick={() => startChat(t, 'hard')}>Hard</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* CHAT */}
      {mode === 'chat' && (
        <div className="card chat-mode">
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px'}}>
            <button className="back-btn" onClick={() => { stopSpeaking(); setMode("menu"); }}>← Back</button>
            <span style={{background: '#eee', color: '#333', padding: '5px 10px', borderRadius: '15px', fontSize: '0.8em', fontWeight: 'bold'}}>
              Mode: {difficulty.toUpperCase()}
            </span>
          </div>
          
          <h2>Explain: <span style={{color: '#646cff'}}>{selectedTopic}</span></h2>
          
          <textarea 
            value={explanation} 
            onChange={(e) => setExplanation(e.target.value)} 
            rows="5" 
            placeholder={`Explain this concept in simple terms (${difficulty} mode)...`}
          />
          
          <div style={{marginTop:'10px', display: 'flex', gap: '10px', flexWrap: 'wrap'}}>
            <button onClick={toggleListening} style={{flex: 1, background: isListening ? '#ff4444' : '#646cff'}}>
              {isListening ? "⏹️ Stop" : "🎤 Speak"}
            </button>

            <button onClick={handleSubmitExplanation} disabled={isGrading || isListening} style={{flex: 1}}>
              {isGrading && !isSpeaking ? "Analyzing..." : "Submit"}
            </button>

            <button onClick={handleToggleAnalogy} style={{flex: 1, background: (isSpeaking || isGrading) ? '#ff9800' : '#28a745'}}>
              {(isSpeaking || isGrading) ? "⏹️ Stop" : "💡 Analogy"}
            </button>
          </div>

          {feedback && (
            <div className="feedback-box">
              <p>{feedback}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default App