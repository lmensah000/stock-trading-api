import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Activity, Apple, Calendar, Users, Utensils, Weight } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function Landing() {
  return (
    <div className="min-h-screen bg-[#09090B] text-white">
      {/* Hero Section */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 z-0">
          <img 
            src="https://images.unsplash.com/photo-1557092738-aa47812cb878"
            alt="Background"
            className="w-full h-full object-cover opacity-30"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-transparent to-[#09090B]"></div>
        </div>
        
        <div className="relative z-10 max-w-7xl mx-auto px-4 py-24 md:py-32">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center"
          >
            <h1 className="text-5xl md:text-7xl font-black uppercase tracking-tight mb-6">
              <span className="text-volt">VOLT</span>FIT
            </h1>
            <p className="text-xl md:text-2xl text-[#A1A1AA] mb-8 max-w-2xl mx-auto">
              High-Performance Fitness Tracking with AI-Powered Nutrition & Community Goals
            </p>
            <div className="flex gap-4 justify-center">
              <Link to="/register">
                <Button 
                  data-testid="get-started-btn"
                  className="bg-[#CCFF00] text-black font-bold uppercase tracking-wide hover:bg-[#B3E600] px-8 py-6 text-lg volt-glow transition-all duration-300"
                >
                  GET STARTED
                </Button>
              </Link>
              <Link to="/login">
                <Button 
                  data-testid="login-btn"
                  variant="outline"
                  className="border-[#27272A] text-white hover:border-[#CCFF00] hover:text-[#CCFF00] uppercase tracking-wide px-8 py-6 text-lg"
                >
                  LOGIN
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-24 px-4 max-w-7xl mx-auto">
        <div className="grid md:grid-cols-3 gap-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="bg-[#18181B] border border-[#27272A] p-8 hover:border-[#CCFF00]/50 transition-all duration-300"
          >
            <Utensils className="w-12 h-12 text-[#CCFF00] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3">AI Meal Planning</h3>
            <p className="text-[#A1A1AA]">
              Generate personalized meal plans based on your available groceries using advanced AI.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="bg-[#18181B] border border-[#27272A] p-8 hover:border-[#CCFF00]/50 transition-all duration-300"
          >
            <Users className="w-12 h-12 text-[#00E0FF] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3">Group Challenges</h3>
            <p className="text-[#A1A1AA]">
              Connect with friends, set group goals, and motivate each other through real-time chat.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
            className="bg-[#18181B] border border-[#27272A] p-8 hover:border-[#CCFF00]/50 transition-all duration-300"
          >
            <Activity className="w-12 h-12 text-[#CCFF00] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3">Multi-Device Sync</h3>
            <p className="text-[#A1A1AA]">
              Track data from Apple Health, Google Fit, and smart scales - all in one place.
            </p>
          </motion.div>
        </div>
      </section>

      {/* Integration Section */}
      <section className="py-24 px-4 bg-[#18181B]/50">
        <div className="max-w-7xl mx-auto">
          <h2 className="text-4xl md:text-5xl font-black uppercase text-center mb-16">
            SEAMLESS <span className="text-volt">INTEGRATION</span>
          </h2>
          <div className="grid md:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-[#27272A] rounded-sm flex items-center justify-center">
                <Apple className="w-10 h-10 text-[#CCFF00]" strokeWidth={1.5} />
              </div>
              <p className="text-[#A1A1AA] uppercase text-sm font-bold tracking-widest">APPLE HEALTH</p>
            </div>
            <div className="text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-[#27272A] rounded-sm flex items-center justify-center">
                <Activity className="w-10 h-10 text-[#00E0FF]" strokeWidth={1.5} />
              </div>
              <p className="text-[#A1A1AA] uppercase text-sm font-bold tracking-widest">GOOGLE FIT</p>
            </div>
            <div className="text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-[#27272A] rounded-sm flex items-center justify-center">
                <Weight className="w-10 h-10 text-[#CCFF00]" strokeWidth={1.5} />
              </div>
              <p className="text-[#A1A1AA] uppercase text-sm font-bold tracking-widest">SMART SCALES</p>
            </div>
            <div className="text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-[#27272A] rounded-sm flex items-center justify-center">
                <Calendar className="w-10 h-10 text-[#00E0FF]" strokeWidth={1.5} />
              </div>
              <p className="text-[#A1A1AA] uppercase text-sm font-bold tracking-widest">MEAL TRACKING</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24 px-4">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-4xl md:text-6xl font-black uppercase mb-6">
            START YOUR <span className="text-volt">TRANSFORMATION</span>
          </h2>
          <p className="text-xl text-[#A1A1AA] mb-8">
            Join thousands of athletes optimizing their performance
          </p>
          <Link to="/register">
            <Button 
              data-testid="cta-register-btn"
              className="bg-[#CCFF00] text-black font-bold uppercase tracking-wide hover:bg-[#B3E600] px-12 py-6 text-lg volt-glow transition-all duration-300"
            >
              CREATE ACCOUNT
            </Button>
          </Link>
        </div>
      </section>
    </div>
  );
}