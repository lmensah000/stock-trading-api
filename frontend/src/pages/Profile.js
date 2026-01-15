import { motion } from 'framer-motion';
import { User, Apple, Activity } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';
import Navbar from '@/components/Navbar';

export default function Profile({ user }) {
  const connectAppleHealth = () => {
    toast.info('Apple Health integration coming soon!');
  };

  const connectGoogleFit = () => {
    toast.info('Google Fit integration coming soon!');
  };

  return (
    <div className="min-h-screen bg-[#09090B]">
      <Navbar />
      
      <div className="max-w-4xl mx-auto px-4 py-8" data-testid="profile-page">
        <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-8">
          YOUR <span className="text-volt">PROFILE</span>
        </h1>

        <div className="space-y-6">
          {/* User Info */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-[#18181B] border border-[#27272A] p-8"
            data-testid="user-info"
          >
            <div className="flex items-center gap-6 mb-6">
              <div className="w-20 h-20 bg-[#27272A] rounded-full flex items-center justify-center">
                <User className="w-10 h-10 text-[#CCFF00]" strokeWidth={1.5} />
              </div>
              <div>
                <h2 className="text-3xl font-black uppercase">{user?.name}</h2>
                <p className="text-[#A1A1AA]">{user?.email}</p>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4 pt-6 border-t border-[#27272A]">
              <div>
                <p className="text-xs text-[#A1A1AA] uppercase font-bold tracking-widest mb-1">Member Since</p>
                <p className="text-white font-bold">
                  {user?.created_at ? new Date(user.created_at).toLocaleDateString() : 'N/A'}
                </p>
              </div>
              <div>
                <p className="text-xs text-[#A1A1AA] uppercase font-bold tracking-widest mb-1">User ID</p>
                <p className="text-white font-mono text-sm">{user?.id?.slice(0, 8)}...</p>
              </div>
            </div>
          </motion.div>

          {/* Fitness Integrations */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-[#18181B] border border-[#27272A] p-8"
            data-testid="integrations"
          >
            <h2 className="text-2xl font-bold uppercase mb-6">FITNESS INTEGRATIONS</h2>
            
            <div className="space-y-4">
              {/* Apple Health */}
              <div className="flex items-center justify-between p-4 bg-[#27272A] border border-[#3F3F46]">
                <div className="flex items-center gap-4">
                  <Apple className="w-8 h-8 text-[#CCFF00]" strokeWidth={1.5} />
                  <div>
                    <h3 className="font-bold uppercase">APPLE HEALTH</h3>
                    <p className="text-sm text-[#A1A1AA]">Sync steps, calories, and activity</p>
                  </div>
                </div>
                <Button
                  onClick={connectAppleHealth}
                  data-testid="connect-apple-health-btn"
                  variant="outline"
                  className="border-[#CCFF00] text-[#CCFF00] hover:bg-[#CCFF00] hover:text-black uppercase"
                >
                  CONNECT
                </Button>
              </div>

              {/* Google Fit */}
              <div className="flex items-center justify-between p-4 bg-[#27272A] border border-[#3F3F46]">
                <div className="flex items-center gap-4">
                  <Activity className="w-8 h-8 text-[#00E0FF]" strokeWidth={1.5} />
                  <div>
                    <h3 className="font-bold uppercase">GOOGLE FIT</h3>
                    <p className="text-sm text-[#A1A1AA]">Track workouts and health metrics</p>
                  </div>
                </div>
                <Button
                  onClick={connectGoogleFit}
                  data-testid="connect-google-fit-btn"
                  variant="outline"
                  className="border-[#00E0FF] text-[#00E0FF] hover:bg-[#00E0FF] hover:text-black uppercase"
                >
                  CONNECT
                </Button>
              </div>
            </div>

            <div className="mt-6 p-4 bg-[#09090B] border border-[#27272A]">
              <p className="text-xs text-[#A1A1AA]">
                <span className="text-[#CCFF00] font-bold">INFRASTRUCTURE READY:</span> The app is prepared for Apple Health and Google Fit integration. Add your API credentials to enable automatic syncing.
              </p>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}