// src/components/AuthModal/SignupForm.js
import React, { useState, useEffect } from 'react';

const SignupForm = () => {
  const [form, setForm] = useState({
    name: '', id: '', password: '', confirmPassword: '',
    school: '', grade: '', email: '', agree: false
  });
  const [gradeOptions, setGradeOptions] = useState([]);

  useEffect(() => {
    if (form.school === '중등' || form.school === '고등') {
      setGradeOptions(['1학년', '2학년', '3학년']);
    } else if (form.school === '초등') {
      setGradeOptions(['1학년', '2학년', '3학년', '4학년', '5학년', '6학년']);
    } else {
      setGradeOptions([]);
    }
  }, [form.school]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = () => {
    console.log('회원가입 정보:', form);
  };

  return (
    <div>
      <h2>회원가입</h2>
      <input name="name" placeholder="이름" onChange={handleChange} />
      <input name="id" placeholder="아이디" onChange={handleChange} />
      <input name="password" type="password" placeholder="비밀번호" onChange={handleChange} />
      <input name="confirmPassword" type="password" placeholder="비밀번호 확인" onChange={handleChange} />
      <div className="row">
        <select name="school" onChange={handleChange}>
          <option value="">학교</option>
          <option value="초등">초등</option>
          <option value="중등">중등</option>
          <option value="고등">고등</option>
        </select>
        <select name="grade" onChange={handleChange}>
          <option value="">학년</option>
          {gradeOptions.map((grade, index) => (
            <option key={index} value={grade}>{grade}</option>
          ))}
        </select>
      </div>
      <input name="email" placeholder="이메일" onChange={handleChange} />
      <div className="checkbox">
        <input type="checkbox" name="agree" onChange={handleChange} />
        <label>이용약관 개인정보 수집 및 정보이용에 동의합니다.</label>
      </div>
      <button className="submit-btn" onClick={handleSubmit}>가입하기</button>
    </div>
  );
};

export default SignupForm;
