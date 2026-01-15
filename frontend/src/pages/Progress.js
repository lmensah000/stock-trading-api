import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Award, TrendingUp, Trophy, Star, Share2, Copy, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

const AVATAR_EMOJIS = ['🌱', '🌿', '🪴', '🌸', '🌺', '🌻', '✨', '🌟'];

const BADGE_EMOJIS = {
  'first_workout': '🏃',
  'workout_warrior': '💪',
  'fitness_master': '🏆',
  'goal_crusher': '🎯',
  'meal_planner': '🍽️',
  'social_butterfly': '🦋',
  'referral_champion': '🌟',
  'consistency_king': '👑',
  'team_player': '🤝'
};

export default function ProgressPage() {
  const [progress, setProgress] = useState(null);
  const [referrals, setReferrals] = useState(null);
  const [allBadges, setAllBadges] = useState([]);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProgress();
    fetchReferrals();
    fetchAllBadges();
  }, []);

  const fetchProgress = async () => {
    try {
      const response = await apiClient.get('/my/progress');
      setProgress(response.data);
    } catch (error) {
      console.error('Failed to fetch progress:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchReferrals = async () => {
    try {
      const response = await apiClient.get('/referrals/my');
      setReferrals(response.data);
    } catch (error) {
      console.error('Failed to fetch referrals:', error);
    }
  };

  const fetchAllBadges = async () => {
    try {
      const response = await apiClient.get('/badges');
      setAllBadges(response.data);
    } catch (error) {
      console.error('Failed to fetch badges:', error);
    }
  };

  const copyReferralCode = () => {
    navigator.clipboard.writeText(referrals?.referral_code || '');
    setCopied(true);
    toast.success('Referral code copied!');
    setTimeout(() => setCopied(false), 2000);
  };

  const shareReferralLink = () => {
    const link = `${window.location.origin}/register?ref=${referrals?.referral_code}`;
    navigator.clipboard.writeText(link);
    toast.success('Referral link copied!');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F5F3EF] flex items-center justify-center">
        <div className="text-[#D4A574] text-2xl font-bold uppercase">LOADING...</div>
      </div>
    );
  }

  const xpProgress = progress?.next_level_info 
    ? ((progress.xp - progress.current_level_info.xp_required) / (progress.next_level_info.xp_required - progress.current_level_info.xp_required)) * 100
    : 100;

  return (
    <div className="min-h-screen bg-[#F5F3EF]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="progress-page">
        <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-8 text-[#5C4A42]">
          YOUR <span className="text-[#D4A574]">PROGRESS</span>
        </h1>

        <div className="grid md:grid-cols-3 gap-8">
          {/* Level & Avatar */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="warm-card p-8 rounded-lg text-center"
          >
            <div className="text-8xl mb-4">{AVATAR_EMOJIS[progress?.avatar_stage - 1] || '🌱'}</div>
            <h2 className="text-3xl font-black uppercase text-[#5C4A42] mb-2">
              Level {progress?.level}
            </h2>
            <p className="text-xl text-[#D4A574] font-bold mb-6">{progress?.current_level_info?.name}</p>
            
            {progress?.next_level_info && (
              <div className="mb-4">
                <div className="flex justify-between text-sm text-[#8B7355] mb-2">
                  <span>{progress.xp} XP</span>
                  <span>{progress.next_level_info.xp_required} XP</span>
                </div>
                <Progress value={xpProgress} className="h-3 bg-[#E8E3DC]" />
                <p className="text-xs text-[#A89284] mt-2">
                  {progress.xp_to_next_level} XP to next level
                </p>
              </div>
            )}
            
            <div className="bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white p-4 rounded-lg">
              <p className="text-xs uppercase tracking-wide mb-1">Next Level Reward</p>
              <p className="text-2xl font-black">+{progress?.next_level_info?.points_reward || 0} pts</p>
            </div>
          </motion.div>

          {/* Badges */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="md:col-span-2 warm-card p-8 rounded-lg"
          >
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-black uppercase text-[#5C4A42]">
                BADGES <Trophy className="inline w-6 h-6 text-[#D4A574]" />
              </h2>
              <p className="text-[#8B7355]">
                {progress?.total_badges} / {progress?.available_badges}
              </p>
            </div>
            
            <div className="grid grid-cols-3 gap-4">
              {allBadges.map((badge) => {
                const earned = progress?.badges?.some(b => b.id === badge.id);
                return (
                  <motion.div
                    key={badge.id}
                    whileHover={{ scale: earned ? 1.05 : 1 }}
                    className={`p-4 rounded-lg text-center transition-all duration-300 ${
                      earned 
                        ? 'bg-gradient-to-br from-[#D4A574] to-[#8B7355] text-white'
                        : 'bg-[#FAF9F7] opacity-50'
                    }`}
                  >
                    <div className="text-4xl mb-2">{BADGE_EMOJIS[badge.id] || badge.icon}</div>
                    <p className="text-xs font-bold uppercase">{badge.name}</p>
                    <p className="text-xs mt-1 opacity-80">{badge.description}</p>
                    <p className="text-xs mt-2 font-bold">+{badge.points_reward} pts</p>
                  </motion.div>
                );
              })}
            </div>
          </motion.div>
        </div>

        {/* Referral Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mt-8 warm-card p-8 rounded-lg"
        >
          <h2 className="text-2xl font-black uppercase mb-6 text-[#5C4A42]">
            REFER FRIENDS <Star className="inline w-6 h-6 text-[#D4A574]" />
          </h2>
          
          <div className="grid md:grid-cols-2 gap-8">
            <div>
              <div className="bg-gradient-to-br from-[#FAF9F7] to-white p-6 rounded-lg mb-4">
                <p className="text-sm text-[#8B7355] mb-2 uppercase tracking-wide">Your Referral Code</p>
                <div className="flex gap-2">
                  <div className="flex-1 bg-white border-2 border-[#D4A574] rounded-lg px-4 py-3 text-2xl font-black text-[#5C4A42] text-center">
                    {referrals?.referral_code || 'Loading...'}
                  </div>
                  <Button
                    onClick={copyReferralCode}
                    className="bg-[#D4A574] text-white hover:bg-[#C19563]"
                  >
                    {copied ? <Check className="w-5 h-5" /> : <Copy className="w-5 h-5" />}
                  </Button>
                </div>
              </div>
              
              <div className="flex gap-2">
                <Button
                  onClick={shareReferralLink}
                  className="flex-1 bg-[#8B7355] text-white hover:bg-[#7A6449]"
                >
                  <Share2 className="w-4 h-4 mr-2" />
                  COPY LINK
                </Button>
                <Button
                  onClick={() => setShareModalOpen(true)}
                  variant="outline"
                  className="flex-1 border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7]"
                >
                  SHARE
                </Button>
              </div>
              
              <div className="mt-4 bg-[#FFF4E6] border border-[#D4A574] p-4 rounded-lg">
                <p className="text-sm text-[#8B7355]">
                  <span className="font-bold text-[#D4A574]">Earn 200 points</span> when a friend:
                </p>
                <ul className="text-xs text-[#8B7355] mt-2 space-y-1">
                  <li>✓ Signs up with your code</li>
                  <li>✓ Completes 1 workout</li>
                  <li>✓ Generates 1 meal plan</li>
                  <li>✓ Creates 1 goal</li>
                </ul>
              </div>
            </div>
            
            <div>
              <div className="grid grid-cols-3 gap-4 mb-4">
                <div className="bg-[#FAF9F7] p-4 rounded-lg text-center">
                  <p className="text-3xl font-black text-[#D4A574]">{referrals?.total_referrals || 0}</p>
                  <p className="text-xs uppercase text-[#8B7355]">Total</p>
                </div>
                <div className="bg-[#FAF9F7] p-4 rounded-lg text-center">
                  <p className="text-3xl font-black text-[#8B7355]">{referrals?.onboarded_referrals || 0}</p>
                  <p className="text-xs uppercase text-[#8B7355]">Onboarded</p>
                </div>
                <div className="bg-gradient-to-br from-[#D4A574] to-[#8B7355] p-4 rounded-lg text-center">
                  <p className="text-3xl font-black text-white">{(referrals?.onboarded_referrals || 0) * 200}</p>
                  <p className="text-xs uppercase text-white">Points Earned</p>
                </div>
              </div>
              
              <div className="max-h-64 overflow-y-auto">
                <h3 className="text-sm font-bold uppercase text-[#5C4A42] mb-3">Referral History</h3>
                {referrals?.referrals?.length > 0 ? (
                  referrals.referrals.map((ref, idx) => (
                    <div key={idx} className="bg-[#FAF9F7] p-3 rounded mb-2 flex justify-between items-center">
                      <div>
                        <p className="text-sm font-bold text-[#5C4A42]">{ref.referee_email}</p>
                        <p className="text-xs text-[#8B7355]">{new Date(ref.created_at).toLocaleDateString()}</p>
                      </div>
                      <span className={`text-xs uppercase font-bold px-2 py-1 rounded ${
                        ref.onboarded ? 'bg-[#D4A574] text-white' : 'bg-[#E8E3DC] text-[#8B7355]'
                      }`}>
                        {ref.status}
                      </span>
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-[#8B7355] text-center py-4">No referrals yet</p>
                )}
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Share Modal */}
      <Dialog open={shareModalOpen} onOpenChange={setShareModalOpen}>
        <DialogContent className="bg-white border border-[#D4C4B0] text-[#5C4A42]">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold uppercase">Share Your Code</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-[#8B7355]">Share your referral code and earn 200 points for each friend who completes onboarding!</p>
            <div className="bg-[#FAF9F7] p-4 rounded-lg text-center">
              <p className="text-3xl font-black text-[#D4A574] mb-2">{referrals?.referral_code}</p>
              <p className="text-sm text-[#8B7355]">Use code at registration</p>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <Button
                onClick={() => {
                  const text = `Join me on Innate Fitness! Use code ${referrals?.referral_code} to get started. Transform from within! 💪`;
                  const url = `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}`;
                  window.open(url, '_blank');
                }}
                className="bg-[#1DA1F2] text-white hover:bg-[#1A8CD8]"
              >
                Twitter
              </Button>
              <Button
                onClick={() => {
                  const text = `Join me on Innate Fitness! Use code ${referrals?.referral_code}`;
                  const url = `https://www.facebook.com/sharer/sharer.php?quote=${encodeURIComponent(text)}`;
                  window.open(url, '_blank');
                }}
                className="bg-[#1877F2] text-white hover:bg-[#1665D8]"
              >
                Facebook
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}