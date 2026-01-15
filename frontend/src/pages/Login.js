import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import apiClient from '@/api';

const LOGO_URL = 'https://customer-assets.emergentagent.com/job_smart-fitness-102/artifacts/xmes01dq_Photoroom_20251009_212303.PNG';

export default function Login({ setToken, setUser }) {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiClient.post('/auth/login', formData);
      const { access_token, user } = response.data;
      
      localStorage.setItem('token', access_token);
      localStorage.setItem('user', JSON.stringify(user));
      setToken(access_token);
      setUser(user);
      
      toast.success('Welcome back!');
      navigate('/dashboard');
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F3EF] flex items-center justify-center px-4 organic-pattern">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <div className="text-center mb-8">
          <img src={LOGO_URL} alt="Innate Fitness" className="h-20 mx-auto mb-4" />
          <h1 className="text-4xl font-black uppercase tracking-tight mb-2 text-[#5C4A42]">
            INNATE FITNESS
          </h1>
          <p className="text-[#8B7355] italic">Login to your account</p>
        </div>

        <div className="warm-card p-8 rounded-lg">
          <form onSubmit={handleSubmit} className="space-y-6" data-testid="login-form">
            <div>
              <Label htmlFor="email" className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                Email
              </Label>
              <Input
                id="email"
                data-testid="email-input"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574] focus:ring-1 focus:ring-[#D4A574]"
                required
              />
            </div>

            <div>
              <Label htmlFor="password" className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                Password
              </Label>
              <Input
                id="password"
                data-testid="password-input"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574] focus:ring-1 focus:ring-[#D4A574]"
                required
              />
            </div>

            <Button
              type="submit"
              data-testid="login-submit-btn"
              disabled={loading}
              className="w-full bg-[#D4A574] text-white font-bold uppercase tracking-wide hover:bg-[#C19563] transition-all duration-300"
            >
              {loading ? 'LOGGING IN...' : 'LOGIN'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-[#8B7355]">
              Don't have an account?{' '}
              <Link to="/register" className="text-[#D4A574] hover:text-[#C19563] font-bold">
                Register
              </Link>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}