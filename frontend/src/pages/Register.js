import { useState } from 'react';
import { motion } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import apiClient from '@/api';

export default function Register({ setToken, setUser }) {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', name: '', password: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiClient.post('/auth/register', formData);
      const { access_token, user } = response.data;
      
      localStorage.setItem('token', access_token);
      localStorage.setItem('user', JSON.stringify(user));
      setToken(access_token);
      setUser(user);
      
      toast.success('Account created successfully!');
      navigate('/dashboard');
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B] flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <div className="text-center mb-8">
          <h1 className="text-5xl font-black uppercase tracking-tight mb-2">
            <span className="text-volt">VOLT</span>FIT
          </h1>
          <p className="text-[#A1A1AA]">Create your account</p>
        </div>

        <div className="bg-[#18181B] border border-[#27272A] p-8">
          <form onSubmit={handleSubmit} className="space-y-6" data-testid="register-form">
            <div>
              <Label htmlFor="name" className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                Name
              </Label>
              <Input
                id="name"
                data-testid="name-input"
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00] focus:ring-1 focus:ring-[#CCFF00]"
                required
              />
            </div>

            <div>
              <Label htmlFor="email" className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                Email
              </Label>
              <Input
                id="email"
                data-testid="email-input"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00] focus:ring-1 focus:ring-[#CCFF00]"
                required
              />
            </div>

            <div>
              <Label htmlFor="password" className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                Password
              </Label>
              <Input
                id="password"
                data-testid="password-input"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00] focus:ring-1 focus:ring-[#CCFF00]"
                required
                minLength={6}
              />
            </div>

            <Button
              type="submit"
              data-testid="register-submit-btn"
              disabled={loading}
              className="w-full bg-[#CCFF00] text-black font-bold uppercase tracking-wide hover:bg-[#B3E600] volt-glow transition-all duration-300"
            >
              {loading ? 'CREATING ACCOUNT...' : 'CREATE ACCOUNT'}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-[#A1A1AA]">
              Already have an account?{' '}
              <Link to="/login" className="text-[#CCFF00] hover:text-[#B3E600] font-bold">
                Login
              </Link>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}