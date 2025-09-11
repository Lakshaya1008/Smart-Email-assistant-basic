// 3. UPDATED src/components/EmailForm.js - COPY PASTE THIS
import React, { useState, useEffect } from 'react';
import { generateEmailReply, testApiConnection } from '../services/api';
import LoadingSpinner from './LoadingSpinner';
import '../styles/EmailForm.css';

const EmailForm = () => {
  const [formData, setFormData] = useState({
    subject: '',
    emailContent: '',
    tone: 'professional'
  });
  const [responseData, setResponseData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [apiStatus, setApiStatus] = useState('checking');
  const [copySuccess, setCopySuccess] = useState('');

  // Test API connection on component mount
  useEffect(() => {
    testApiConnection()
      .then(() => setApiStatus('connected'))
      .catch(() => setApiStatus('disconnected'));
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.subject.trim() || !formData.emailContent.trim()) {
      setError('Both subject and email content are required');
      return;
    }

    setLoading(true);
    setError('');
    setResponseData(null);
    setCopySuccess('');

    // Debug log
    console.log('Sending request:', formData);

    try {
      const response = await generateEmailReply(formData);
      console.log('Received response:', response);
      setResponseData(response);
    } catch (err) {
      console.error('Error:', err);
      setError(err.message || 'Failed to generate email reply');
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setFormData({ subject: '', emailContent: '', tone: 'professional' });
    setResponseData(null);
    setError('');
    setCopySuccess('');
  };

  const copyToClipboard = async (text, type) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopySuccess(`${type} copied to clipboard!`);
      setTimeout(() => setCopySuccess(''), 3000);
    } catch (err) {
      console.error('Failed to copy:', err);
      setCopySuccess('Failed to copy to clipboard');
    }
  };

  return (
    <div className="email-form-container">
      {/* API Status Indicator */}
      <div className={`api-status ${apiStatus}`}>
        <span className={`status-indicator ${apiStatus}`}></span>
        Backend Status: {apiStatus === 'checking' ? 'Checking...' : apiStatus === 'connected' ? 'Connected (Port 8081)' : 'Disconnected'}
      </div>

      <form onSubmit={handleSubmit} className="email-form">
        <div className="form-group">
          <label htmlFor="subject" className="form-label">
            Email Subject *
          </label>
          <input
            type="text"
            id="subject"
            name="subject"
            value={formData.subject}
            onChange={handleInputChange}
            className="form-input"
            placeholder="Enter the email subject..."
            disabled={loading}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="emailContent" className="form-label">
            Email Content *
          </label>
          <textarea
            id="emailContent"
            name="emailContent"
            value={formData.emailContent}
            onChange={handleInputChange}
            className="form-textarea"
            placeholder="Enter the original email content..."
            rows="6"
            disabled={loading}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="tone" className="form-label">
            Reply Tone
          </label>
          <select
            id="tone"
            name="tone"
            value={formData.tone}
            onChange={handleInputChange}
            className="form-select"
            disabled={loading}
          >
            <option value="professional">Professional</option>
            <option value="casual">Casual</option>
            <option value="formal">Formal</option>
            <option value="friendly">Friendly</option>
            <option value="apologetic">Apologetic</option>
            <option value="assertive">Assertive</option>
          </select>
        </div>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            {error}
          </div>
        )}

        {copySuccess && (
          <div className="success-message">
            <span className="success-icon">✅</span>
            {copySuccess}
          </div>
        )}

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || apiStatus === 'disconnected'}
          >
            {loading ? 'Generating...' : '✨ Generate Reply'}
          </button>
          <button
            type="button"
            onClick={handleClear}
            className="btn btn-secondary"
            disabled={loading}
          >
            🗑️ Clear
          </button>
        </div>
      </form>

      {loading && (
        <div className="loading-container">
          <LoadingSpinner />
          <p>🤖 AI is analyzing your email and generating a {formData.tone} reply...</p>
        </div>
      )}

      {responseData && (
        <div className="results-container">
          {/* Summary Section */}
          {responseData.summary && responseData.summary.trim() && (
            <div className="summary-container">
              <div className="summary-header">
                <h3>📋 Email Summary</h3>
                <button
                  onClick={() => copyToClipboard(responseData.summary, 'Summary')}
                  className="btn btn-outline btn-small"
                  title="Copy summary"
                >
                  📋 Copy
                </button>
              </div>
              <div className="summary-content">
                {responseData.summary}
              </div>
            </div>
          )}

          {/* Reply Section */}
          <div className="reply-container">
            <div className="reply-header">
              <h3>✉️ Generated Reply ({formData.tone} tone)</h3>
              <button
                onClick={() => copyToClipboard(
                  typeof responseData === 'string' ? responseData : responseData.reply,
                  'Reply'
                )}
                className="btn btn-outline btn-small"
                title="Copy reply"
              >
                📋 Copy
              </button>
            </div>
            <div className="reply-content">
              {typeof responseData === 'string' ? responseData : responseData.reply}
            </div>
          </div>

          {/* Copy All Button */}
          <div className="copy-all-container">
            <button
              onClick={() => copyToClipboard(
                (responseData.summary && responseData.summary.trim() ? 
                  `Summary: ${responseData.summary}\n\n` : '') +
                `Reply: ${typeof responseData === 'string' ? responseData : responseData.reply}`,
                'All content'
              )}
              className="btn btn-primary"
            >
              📋 Copy All
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmailForm;
