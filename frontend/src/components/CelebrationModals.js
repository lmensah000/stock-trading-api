import { motion } from 'framer-motion';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Sparkles, Award } from 'lucide-react';

const AVATAR_EMOJIS = ['🌱', '🌿', '🪴', '🌸', '🌺', '🌻', '✨', '🌟'];

export function LevelUpModal({ open, onClose, levelData, xp }) {
  if (!levelData) return null;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="bg-white border-4 border-[#D4A574] max-w-md">
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="text-center py-6"
        >
          <motion.div
            animate={{ 
              scale: [1, 1.2, 1],
              rotate: [0, 10, -10, 0]
            }}
            transition={{ duration: 0.6, repeat: 2 }}
            className="text-8xl mb-4"
          >
            {AVATAR_EMOJIS[levelData.avatar_stage - 1]}
          </motion.div>
          
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            <h2 className="text-4xl font-black uppercase text-[#5C4A42] mb-2">
              LEVEL UP!
            </h2>
            <p className="text-2xl font-bold text-[#D4A574] mb-4">
              Level {levelData.level}: {levelData.name}
            </p>
            
            <div className="bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white p-6 rounded-lg mb-6">
              <Sparkles className="w-8 h-8 mx-auto mb-2" />
              <p className="text-sm uppercase tracking-wide mb-1">Reward Unlocked</p>
              <p className="text-3xl font-black">+{levelData.points_reward} Points</p>
            </div>
            
            <p className="text-[#8B7355] mb-6">
              You now have <span className="font-bold text-[#D4A574]">{xp} XP</span>
            </p>
            
            <Button
              onClick={onClose}
              className="bg-[#D4A574] text-white font-bold uppercase px-8 py-6 hover:bg-[#C19563]"
            >
              CONTINUE GROWING
            </Button>
          </motion.div>
        </motion.div>
      </DialogContent>
    </Dialog>
  );
}

export function BadgeEarnedModal({ open, onClose, badge }) {
  if (!badge) return null;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="bg-white border-4 border-[#8B7355] max-w-md">
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="text-center py-6"
        >
          <motion.div
            animate={{ 
              rotate: [0, -10, 10, -10, 10, 0],
              scale: [1, 1.1, 1]
            }}
            transition={{ duration: 0.8, repeat: 1 }}
            className="text-8xl mb-4"
          >
            {badge.icon}
          </motion.div>
          
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            <h2 className="text-4xl font-black uppercase text-[#5C4A42] mb-2">
              BADGE EARNED!
            </h2>
            <p className="text-2xl font-bold text-[#8B7355] mb-4">
              {badge.name}
            </p>
            
            <p className="text-[#8B7355] mb-6">
              {badge.description}
            </p>
            
            <div className="bg-gradient-to-r from-[#8B7355] to-[#D4A574] text-white p-6 rounded-lg mb-6">
              <Award className="w-8 h-8 mx-auto mb-2" />
              <p className="text-sm uppercase tracking-wide mb-1">Reward</p>
              <p className="text-3xl font-black">+{badge.points_reward} Points</p>
            </div>
            
            <Button
              onClick={onClose}
              className="bg-[#8B7355] text-white font-bold uppercase px-8 py-6 hover:bg-[#7A6449]"
            >
              AWESOME!
            </Button>
          </motion.div>
        </motion.div>
      </DialogContent>
    </Dialog>
  );
}
