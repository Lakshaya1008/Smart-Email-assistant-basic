import React from 'react';
import Navbar from './components/Navbar';
import EmailForm from './components/EmailForm';

function App() {
  return (
    <div className="App">
      <Navbar />
      <main className="main-content">
        <div className="container">
          <h1 className="page-title">Generate Smart Email Replies</h1>
          <p className="page-subtitle">
            Enter your email details below and let AI craft the perfect response
          </p>
          <EmailForm />
        </div>
      </main>
    </div>
  );
}

export default App;