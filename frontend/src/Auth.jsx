import { useState } from 'react';
import './App.css'; // Re-using your existing styles

const API_BASE = "http://localhost:8080/api/auth";

function Auth({ onLogin }) {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const endpoint = isLogin ? "/login" : "/signup";
    const payload = isLogin ? { email, password } : { email, password, name };

    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (!res.ok) throw new Error(data.error || "Something went wrong");

      if (isLogin) {
        // Login Success: Save token and notify App
        localStorage.setItem("token", data.token);
        localStorage.setItem("user", JSON.stringify(data.user));
        onLogin(data.user);
      } else {
        // Signup Success: Switch to login view
        setIsLogin(true);
        setError("Account created! Please log in.");
        setLoading(false); // Stop loading so they can type
      }

    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '50px' }}>
      <h1>{isLogin ? "Welcome Back" : "Join Feynmind"}</h1>
      
      <div className="card">
        {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}
        
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          
          {!isLogin && (
            <input 
              type="text" 
              placeholder="Full Name" 
              autoComplete="name" 
              value={name} 
              onChange={(e) => setName(e.target.value)} 
              required 
            />
          )}

          <input 
            type="email" 
            placeholder="Email Address" 
            autoComplete="email" 
            value={email} 
            onChange={(e) => setEmail(e.target.value)} 
            required 
          />

          <input 
            type="password" 
            placeholder="Password" 
            autoComplete="current-password" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            required 
          />

          <button type="submit" disabled={loading}>
            {loading ? "Processing..." : (isLogin ? "Log In" : "Sign Up")}
          </button>
        </form>

        <p style={{ marginTop: '15px', fontSize: '0.9em' }}>
          {isLogin ? "New here?" : "Already have an account?"} 
          <span 
            onClick={() => { setIsLogin(!isLogin); setError(""); }} 
            style={{ color: '#646cff', cursor: 'pointer', fontWeight: 'bold', marginLeft: '5px' }}
          >
            {isLogin ? "Create Account" : "Log In"}
          </span>
        </p>
      </div>
    </div>
  );
}

export default Auth;