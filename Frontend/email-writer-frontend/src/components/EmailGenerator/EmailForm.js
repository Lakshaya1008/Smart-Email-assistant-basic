import React, { useState } from 'react';
import { EMAIL_TONES, LANGUAGE_OPTIONS } from '../../utils/constants';
import LoadingSpinner from '../Common/LoadingSpinner';
import './EmailGenerator.css';

const EmailForm = ({ onGenerate, loading }) => {
  const [formData, setFormData] = useState({
    subject: '',
    emailContent: '',
    tone: 'professional',
    language: 'en'
  });
  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.subject.trim()) {
      newErrors.subject = 'Email subject is required';
    }

    if (!formData.emailContent.trim()) {
      newErrors.emailContent = 'Email content is required';
    } else if (formData.emailContent.trim().length < 10) {
      newErrors.emailContent = 'Email content should be at least 10 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    onGenerate({
      subject: formData.subject.trim(),
      emailContent: formData.emailContent.trim(),
      tone: formData.tone,
      language: formData.language
    });
  };

  const handleClear = () => {
    setFormData({
      subject: '',
      emailContent: '',
      tone: 'professional',
      language: 'en'
    });
    setErrors({});
  };

  return (
    <div className="email-form-container">
      <div className="form-header">
        <h2 className="form-title">
          <i className="fas fa-envelope"></i>
          Original Email
        </h2>
        <p className="form-description">
          Paste the email you want to respond to, and we'll generate multiple reply options.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="email-form">
        <div className="form-group">
          <label htmlFor="subject" className="form-label">
            Email Subject
          </label>
          <input
            type="text"
            id="subject"
            name="subject"
            value={formData.subject}
            onChange={handleChange}
            className={`form-input ${errors.subject ? 'error' : ''}`}
            placeholder="e.g., Meeting Request, Project Update, Follow-up..."
            disabled={loading}
          />
          {errors.subject && (
            <div className="form-error">{errors.subject}</div>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="emailContent" className="form-label">
            Email Content
          </label>
          <textarea
            id="emailContent"
            name="emailContent"
            value={formData.emailContent}
            onChange={handleChange}
            className={`form-input form-textarea ${errors.emailContent ? 'error' : ''}`}
            placeholder="Paste the original email content here..."
            rows="6"
            disabled={loading}
          />
          {errors.emailContent && (
            <div className="form-error">{errors.emailContent}</div>
          )}
          <div className="form-hint">
            <i className="fas fa-info-circle"></i>
            Copy and paste the entire email content for best results
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="tone" className="form-label">
              Reply Tone
            </label>
            <select
              id="tone"
              name="tone"
              value={formData.tone}
              onChange={handleChange}
              className="form-select"
              disabled={loading}
            >
              {EMAIL_TONES.map(tone => (
                <option key={tone.value} value={tone.value}>
                  {tone.label}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="language" className="form-label">
              Language
            </label>
            <select
              id="language"
              name="language"
              value={formData.language}
              onChange={handleChange}
              className="form-select"
              disabled={loading}
            >
              {LANGUAGE_OPTIONS.map(lang => (
                <option key={lang.value} value={lang.value}>
                  {lang.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="form-actions">
          <button
            type="button"
            onClick={handleClear}
            className="btn btn-secondary"
            disabled={loading}
          >
            <i className="fas fa-eraser"></i>
            Clear
          </button>
          
          <button
            type="submit"
            className="btn btn-primary btn-large"
            disabled={loading}
          >
            {loading ? (
              <>
                <LoadingSpinner size="small" />
                Generating...
              </>
            ) : (
              <>
                <i className="fas fa-magic"></i>
                Generate Replies
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default EmailForm;