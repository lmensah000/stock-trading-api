import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Share2, Facebook, Twitter, Linkedin, Link as LinkIcon, Check } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';
import apiClient from '@/api';

export default function ShareModal({ achievement, open, onClose }) {
  const [copied, setCopied] = useState(false);
  const shareUrl = `https://innate.fitness/share/${achievement?.id}`;
  const shareText = `I just achieved: ${achievement?.title} on Innate Fitness! 💪 #InnateFitness #TransformFromWithin`;

  const handleShare = async (platform) => {
    try {
      await apiClient.post('/achievements/share', {
        achievement_id: achievement.id,
        platform
      });
      
      toast.success('Achievement shared! +5 points earned');
      
      // Update user points
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const response = await apiClient.get('/auth/me');
        localStorage.setItem('user', JSON.stringify(response.data));
      }
      
      onClose();
    } catch (error) {
      console.error('Failed to share:', error);
    }
  };

  const copyLink = () => {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const shareToFacebook = () => {
    handleShare('facebook');
    window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`, '_blank');
  };

  const shareToTwitter = () => {
    handleShare('twitter');
    window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(shareText)}&url=${encodeURIComponent(shareUrl)}`, '_blank');
  };

  const shareToLinkedIn = () => {
    handleShare('linkedin');
    window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(shareUrl)}`, '_blank');
  };

  if (!achievement) return null;

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="bg-white border border-[#D4C4B0] text-[#5C4A42]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold uppercase flex items-center gap-2">
            <Share2 className="w-6 h-6 text-[#D4A574]" />
            Share Achievement
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4">
          <div className="warm-card p-6 rounded-lg text-center">
            <div className="text-5xl mb-3">🏆</div>
            <h3 className="text-xl font-bold text-[#5C4A42] mb-2">{achievement.title}</h3>
            <p className="text-[#8B7355] text-sm">{achievement.description}</p>
          </div>
          
          <div className="bg-[#FAF9F7] p-4 rounded-lg text-center">
            <p className="text-sm text-[#8B7355] mb-2">Share and earn <span className="font-bold text-[#D4A574]">+5 points</span></p>
          </div>
          
          <div className="grid grid-cols-3 gap-3">
            <Button
              onClick={shareToFacebook}
              className="flex flex-col items-center gap-2 h-auto py-4 bg-[#1877F2] hover:bg-[#1665D8] text-white"
            >
              <Facebook className="w-6 h-6" />
              <span className="text-xs">Facebook</span>
            </Button>
            
            <Button
              onClick={shareToTwitter}
              className="flex flex-col items-center gap-2 h-auto py-4 bg-[#1DA1F2] hover:bg-[#1A8CD8] text-white"
            >
              <Twitter className="w-6 h-6" />
              <span className="text-xs">Twitter</span>
            </Button>
            
            <Button
              onClick={shareToLinkedIn}
              className="flex flex-col items-center gap-2 h-auto py-4 bg-[#0A66C2] hover:bg-[#0952A5] text-white"
            >
              <Linkedin className="w-6 h-6" />
              <span className="text-xs">LinkedIn</span>
            </Button>
          </div>
          
          <div className="flex gap-2">
            <input
              type="text"
              value={shareUrl}
              readOnly
              className="flex-1 bg-[#FAF9F7] border border-[#D4C4B0] rounded px-3 py-2 text-sm text-[#8B7355]"
            />
            <Button
              onClick={copyLink}
              variant="outline"
              className="border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7]"
            >
              {copied ? <Check className="w-4 h-4" /> : <LinkIcon className="w-4 h-4" />}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}