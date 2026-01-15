import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Activity, Apple, Target, Users, Utensils, Weight, Award, Dumbbell } from 'lucide-react';
import { Button } from '@/components/ui/button';

const LOGO_URL = 'https://customer-assets.emergentagent.com/job_smart-fitness-102/artifacts/xmes01dq_Photoroom_20251009_212303.PNG';

export default function Landing() {
  return (
    <div className="min-h-screen bg-[#F5F3EF]">
      {/* Hero Section */}
      <section className="relative overflow-hidden organic-pattern">
        <div className="absolute inset-0 z-0">
          <img 
            src="https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b"
            alt="Fitness Background"
            className="w-full h-full object-cover opacity-20"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-[#F5F3EF]/50 to-[#F5F3EF]"></div>
        </div>
        
        <div className="relative z-10 max-w-7xl mx-auto px-4 py-16 md:py-24">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center"
          >
            <div className="flex justify-center mb-6">
              <img src={LOGO_URL} alt="Innate Fitness" className="h-24 md:h-32" />
            </div>
            <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-4 text-[#5C4A42]">
              INNATE FITNESS
            </h1>
            <p className="text-xl md:text-2xl text-[#8B7355] mb-2 italic font-light">
              Because it all starts from within
            </p>
            <p className="text-lg text-[#A89284] mb-8 max-w-2xl mx-auto">
              AI-Powered Nutrition, Workout Tracking, Community Goals & Rewards
            </p>
            <div className="flex gap-4 justify-center flex-wrap">
              <Link to="/register">
                <Button 
                  data-testid="get-started-btn"
                  className="bg-[#D4A574] text-white font-bold uppercase tracking-wide hover:bg-[#C19563] px-8 py-6 text-lg innate-shadow transition-all duration-300"
                >
                  GET STARTED
                </Button>
              </Link>
              <Link to="/login">
                <Button 
                  data-testid="login-btn"
                  variant="outline"
                  className="border-2 border-[#8B7355] text-[#8B7355] hover:bg-[#8B7355] hover:text-white uppercase tracking-wide px-8 py-6 text-lg transition-all duration-300"
                >
                  LOGIN
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-4 max-w-7xl mx-auto">
        <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 text-[#5C4A42]">
          Everything You Need to Transform From Within
        </h2>
        <div className="grid md:grid-cols-3 gap-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="warm-card p-8 rounded-lg"
          >
            <Utensils className="w-12 h-12 text-[#D4A574] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">AI Meal Planning</h3>
            <p className="text-[#8B7355]">
              Generate personalized meal plans based on your available groceries using Gemini AI.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="warm-card p-8 rounded-lg"
          >
            <Dumbbell className="w-12 h-12 text-[#8B7355] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">Workout Library</h3>
            <p className="text-[#8B7355]">
              Access guided workout videos, track your sessions, and earn points for every workout completed.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
            className="warm-card p-8 rounded-lg"
          >
            <Award className="w-12 h-12 text-[#D4A574] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">Rewards System</h3>
            <p className="text-[#8B7355]">
              Earn points from workouts and challenges. Redeem for apparel, supplements, and more.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.3 }}
            className="warm-card p-8 rounded-lg"
          >
            <Users className="w-12 h-12 text-[#8B7355] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">Group Challenges</h3>
            <p className="text-[#8B7355]">
              Connect with friends, set group goals, and motivate each other through real-time chat.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.4 }}
            className="warm-card p-8 rounded-lg"
          >
            <Target className="w-12 h-12 text-[#D4A574] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">Goal Tracking</h3>
            <p className="text-[#8B7355]">
              Set personal fitness goals, track progress, and share achievements with your community.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.5 }}
            className="warm-card p-8 rounded-lg"
          >
            <Activity className="w-12 h-12 text-[#8B7355] mb-4" strokeWidth={1.5} />
            <h3 className="text-2xl font-bold uppercase mb-3 text-[#5C4A42]">Multi-Device Sync</h3>
            <p className="text-[#8B7355]">
              Track data from Apple Health, Google Fit, and smart scales - all in one place.
            </p>
          </motion.div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 bg-gradient-to-br from-[#D4A574] to-[#8B7355] text-white">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-4xl md:text-6xl font-black uppercase mb-6">
            Start Your Journey Within
          </h2>
          <p className="text-xl mb-8 opacity-90">
            Join the Innate Fitness community and unlock your true potential
          </p>
          <Link to="/register">
            <Button 
              data-testid="cta-register-btn"
              className="bg-white text-[#8B7355] font-bold uppercase tracking-wide hover:bg-[#F5F3EF] px-12 py-6 text-lg transition-all duration-300"
            >
              CREATE ACCOUNT
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 px-4 bg-[#FAF9F7] border-t border-[#D4C4B0]">
        <div className="max-w-7xl mx-auto text-center text-[#8B7355]">
          <p className="text-sm">&copy; 2025 Innate Fitness. Because it all starts from within.</p>
        </div>
      </footer>
    </div>
  );
}