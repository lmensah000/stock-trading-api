import { motion, AnimatePresence } from 'framer-motion';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Award, Dumbbell, Target, Trophy, Star } from 'lucide-react';

const LOGO_URL = 'https://customer-assets.emergentagent.com/job_smart-fitness-102/artifacts/xmes01dq_Photoroom_20251009_212303.PNG';

export default function WelcomeModal({ open, onClose }) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="bg-white border-2 border-[#D4A574] max-w-2xl">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center py-4"
        >
          <img src={LOGO_URL} alt="Innate Fitness" className="h-20 mx-auto mb-4" />
          <h2 className="text-3xl font-black uppercase mb-2 text-[#5C4A42]">
            Welcome to Innate Fitness!
          </h2>
          <p className="text-[#8B7355] italic mb-6">Because it all starts from within</p>
          
          <div className="bg-gradient-to-br from-[#FAF9F7] to-white p-6 rounded-lg mb-6">
            <h3 className="text-xl font-bold uppercase mb-4 text-[#5C4A42] flex items-center justify-center gap-2">
              <Award className="w-6 h-6 text-[#D4A574]" />
              Earn Rewards as You Transform
            </h3>
            
            <div className="grid md:grid-cols-2 gap-4 text-left">
              <div className="flex gap-3 items-start">
                <div className="w-10 h-10 rounded-full bg-[#D4A574] flex items-center justify-center flex-shrink-0">
                  <Dumbbell className="w-5 h-5 text-white" />
                </div>
                <div>
                  <p className="font-bold text-[#5C4A42] text-sm uppercase">Complete Workouts</p>
                  <p className="text-xs text-[#8B7355]">Earn 10-50 points per workout based on duration</p>
                </div>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-10 h-10 rounded-full bg-[#8B7355] flex items-center justify-center flex-shrink-0">
                  <Target className="w-5 h-5 text-white" />
                </div>
                <div>
                  <p className="font-bold text-[#5C4A42] text-sm uppercase">Achieve Goals</p>
                  <p className="text-xs text-[#8B7355]">Earn 50 points when you complete a fitness goal</p>
                </div>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-10 h-10 rounded-full bg-[#D4A574] flex items-center justify-center flex-shrink-0">
                  <Trophy className="w-5 h-5 text-white" />
                </div>
                <div>
                  <p className="font-bold text-[#5C4A42] text-sm uppercase">Group Challenges</p>
                  <p className="text-xs text-[#8B7355]">Earn 20 points for contributing to group goals</p>
                </div>
              </div>
              
              <div className="flex gap-3 items-start">
                <div className="w-10 h-10 rounded-full bg-[#8B7355] flex items-center justify-center flex-shrink-0">
                  <Star className="w-5 h-5 text-white" />
                </div>
                <div>
                  <p className="font-bold text-[#5C4A42] text-sm uppercase">Share Success</p>
                  <p className="text-xs text-[#8B7355]">Earn 5 points when you share achievements</p>
                </div>
              </div>
            </div>
          </div>
          
          <div className="bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white p-4 rounded-lg mb-6">
            <p className="font-bold uppercase text-sm mb-1">Redeem Points for Rewards!</p>
            <p className="text-xs opacity-90">Shop for premium apparel, supplements, protein bars, and equipment</p>
          </div>
          
          <Button
            onClick={onClose}
            className="bg-[#D4A574] text-white font-bold uppercase px-8 py-6 text-lg hover:bg-[#C19563] w-full"
          >
            START YOUR JOURNEY
          </Button>
        </motion.div>
      </DialogContent>
    </Dialog>
  );
}