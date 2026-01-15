import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plus, Send, TrendingUp, Users as UsersIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { ScrollArea } from '@/components/ui/scroll-area';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function GroupGoals({ user }) {
  const [groups, setGroups] = useState([]);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [messages, setMessages] = useState([]);
  const [rankings, setRankings] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [open, setOpen] = useState(false);
  const [newGroup, setNewGroup] = useState({
    name: '',
    description: '',
    target_value: '',
    unit: ''
  });

  useEffect(() => {
    fetchGroups();
  }, []);

  useEffect(() => {
    if (selectedGroup) {
      fetchMessages(selectedGroup.id);
      fetchRankings(selectedGroup.id);
      const interval = setInterval(() => {
        fetchMessages(selectedGroup.id);
        fetchRankings(selectedGroup.id);
      }, 5000);
      return () => clearInterval(interval);
    }
  }, [selectedGroup]);

  const fetchGroups = async () => {
    try {
      const response = await apiClient.get('/groups');
      setGroups(response.data);
    } catch (error) {
      console.error('Failed to fetch groups:', error);
    }
  };

  const fetchMessages = async (groupId) => {
    try {
      const response = await apiClient.get(`/groups/${groupId}/messages`);
      setMessages(response.data);
    } catch (error) {
      console.error('Failed to fetch messages:', error);
    }
  };

  const createGroup = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/groups', {
        ...newGroup,
        target_value: parseFloat(newGroup.target_value)
      });
      
      toast.success('Group created!');
      setOpen(false);
      setNewGroup({ name: '', description: '', target_value: '', unit: '' });
      fetchGroups();
    } catch (error) {
      toast.error('Failed to create group');
    }
  };

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim()) return;

    try {
      await apiClient.post(`/groups/${selectedGroup.id}/messages`, {
        content: newMessage
      });
      setNewMessage('');
      fetchMessages(selectedGroup.id);
    } catch (error) {
      toast.error('Failed to send message');
    }
  };

  const updateGroupProgress = async (groupId, progress) => {
    try {
      await apiClient.patch(`/groups/${groupId}/progress`, null, {
        params: { progress: parseFloat(progress) }
      });
      toast.success('Progress updated!');
      fetchGroups();
      fetchRankings(groupId);
      if (selectedGroup?.id === groupId) {
        const updatedGroup = groups.find(g => g.id === groupId);
        setSelectedGroup({ ...updatedGroup, current_value: updatedGroup.current_value + parseFloat(progress) });
      }
    } catch (error) {
      toast.error('Failed to update progress');
    }
  };

  const fetchRankings = async (groupId) => {
    try {
      const response = await apiClient.get(`/groups/${groupId}/rankings`);
      setRankings(response.data);
    } catch (error) {
      console.error('Failed to fetch rankings:', error);
    }
  };

  const completeGroupGoal = async (groupId) => {
    try {
      const response = await apiClient.post(`/groups/${groupId}/complete`);
      toast.success('Group goal completed! Check your rewards!');
      fetchGroups();
      setSelectedGroup(null);
      
      // Show rankings result
      if (response.data.rankings) {
        const userRanking = response.data.rankings.find(r => r.user_id === user?.id);
        if (userRanking) {
          toast.success(`You ranked #${userRanking.rank}! +${userRanking.points_earned} points earned!`, {
            duration: 5000
          });
        }
      }
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Failed to complete group goal');
    }
  };

  return (
    <div className="min-h-screen bg-[#FAF9F7]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="group-goals-page">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight">
            GROUP <span className="text-volt">CHALLENGES</span>
          </h1>
          
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button 
                data-testid="create-group-btn"
                className="bg-[#D4A574] text-black font-bold uppercase hover:bg-[#B3E600]"
              >
                <Plus className="w-4 h-4 mr-2" />
                NEW GROUP
              </Button>
            </DialogTrigger>
            <DialogContent className="warm-card border border-[#D4C4B0] text-white">
              <DialogHeader>
                <DialogTitle className="text-2xl font-bold uppercase">CREATE GROUP</DialogTitle>
              </DialogHeader>
              <form onSubmit={createGroup} className="space-y-4" data-testid="group-form">
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Group Name
                  </Label>
                  <Input
                    data-testid="group-name-input"
                    value={newGroup.name}
                    onChange={(e) => setNewGroup({ ...newGroup, name: e.target.value })}
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                    required
                  />
                </div>
                
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Description
                  </Label>
                  <Textarea
                    data-testid="group-description-input"
                    value={newGroup.description}
                    onChange={(e) => setNewGroup({ ...newGroup, description: e.target.value })}
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                    required
                  />
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                      Target Value
                    </Label>
                    <Input
                      data-testid="group-target-input"
                      type="number"
                      value={newGroup.target_value}
                      onChange={(e) => setNewGroup({ ...newGroup, target_value: e.target.value })}
                      className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                      required
                    />
                  </div>
                  <div>
                    <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                      Unit
                    </Label>
                    <Input
                      data-testid="group-unit-input"
                      value={newGroup.unit}
                      onChange={(e) => setNewGroup({ ...newGroup, unit: e.target.value })}
                      placeholder="km, steps"
                      className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                      required
                    />
                  </div>
                </div>
                
                <Button
                  type="submit"
                  data-testid="submit-group-btn"
                  className="w-full bg-[#D4A574] text-black font-bold uppercase hover:bg-[#B3E600]"
                >
                  CREATE GROUP
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {/* Groups List */}
          <div className="md:col-span-1 space-y-4" data-testid="groups-list">
            {groups.length === 0 ? (
              <div className="warm-card border border-[#D4C4B0] p-6 text-center">
                <p className="text-[#8B7355]">No groups yet</p>
              </div>
            ) : (
              groups.map((group) => {
                const progress = (group.current_value / group.target_value) * 100;
                return (
                  <motion.div
                    key={group.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    onClick={() => setSelectedGroup(group)}
                    className={`warm-card border p-4 cursor-pointer transition-all duration-300 ${
                      selectedGroup?.id === group.id ? 'border-[#D4A574]' : 'border-[#D4C4B0] hover:border-[#D4A574]/50'
                    }`}
                    data-testid={`group-${group.id}`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <UsersIcon className="w-5 h-5 text-[#D4A574]" strokeWidth={1.5} />
                      <h3 className="font-bold uppercase">{group.name}</h3>
                    </div>
                    <p className="text-[#8B7355] text-sm mb-3">{group.description}</p>
                    <div className="mb-2">
                      <div className="flex items-center justify-between text-sm mb-1">
                        <span>{group.current_value} / {group.target_value} {group.unit}</span>
                        <span className="text-[#D4A574] font-bold">{progress.toFixed(0)}%</span>
                      </div>
                      <Progress value={progress} className="h-1.5 bg-[#27272A]" />
                    </div>
                    <p className="text-[#8B7355] text-xs">{group.members.length} members</p>
                  </motion.div>
                );
              })
            )}
          </div>

          {/* Group Detail & Chat */}
          <div className="md:col-span-2">
            {selectedGroup ? (
              <div className="warm-card border border-[#D4C4B0] h-[600px] flex flex-col" data-testid="group-detail">
                {/* Header */}
                <div className="p-6 border-b border-[#D4C4B0]">
                  <h2 className="text-2xl font-bold uppercase mb-2">{selectedGroup.name}</h2>
                  <p className="text-[#8B7355] mb-4">{selectedGroup.description}</p>
                  
                  <div className="flex items-center gap-4 mb-4">
                    <div>
                      <p className="text-xs text-[#8B7355] uppercase">Progress</p>
                      <p className="text-xl font-bold">
                        {selectedGroup.current_value} / {selectedGroup.target_value} {selectedGroup.unit}
                      </p>
                    </div>
                    <div className="flex-1">
                      <Progress 
                        value={(selectedGroup.current_value / selectedGroup.target_value) * 100} 
                        className="h-3 bg-[#27272A]" 
                      />
                    </div>
                  </div>
                  
                  <div className="flex gap-2 mb-4">
                    <Input
                      type="number"
                      placeholder="Add your progress"
                      data-testid="add-progress-input"
                      className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574]"
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          updateGroupProgress(selectedGroup.id, e.target.value);
                          e.target.value = '';
                        }
                      }}
                    />
                    <Button
                      onClick={(e) => {
                        const input = e.target.closest('.flex').querySelector('input');
                        if (input.value) {
                          updateGroupProgress(selectedGroup.id, input.value);
                          input.value = '';
                        }
                      }}
                      className="bg-[#D4A574] text-white font-bold uppercase hover:bg-[#C19563] whitespace-nowrap"
                      data-testid="update-group-progress-btn"
                    >
                      <TrendingUp className="w-4 h-4" />
                    </Button>
                  </div>
                  
                  {/* Complete Goal Button */}
                  {selectedGroup.current_value >= selectedGroup.target_value && (
                    <Button
                      onClick={() => completeGroupGoal(selectedGroup.id)}
                      className="w-full bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white font-bold uppercase hover:opacity-90"
                      data-testid="complete-group-goal-btn"
                    >
                      🏆 COMPLETE GOAL & DISTRIBUTE REWARDS
                    </Button>
                  )}
                  
                  {/* Leaderboard */}
                  {rankings.length > 0 && (
                    <div className="mt-4 bg-[#FAF9F7] p-4 rounded-lg">
                      <h3 className="text-sm font-bold uppercase text-[#5C4A42] mb-3 flex items-center gap-2">
                        🏅 LEADERBOARD
                      </h3>
                      <div className="space-y-2">
                        {rankings.map((ranking, idx) => (
                          <div
                            key={ranking.user_id}
                            className={`flex items-center justify-between p-2 rounded ${ranking.user_id === user?.id ? 'bg-[#D4A574] text-white' : 'bg-white'}`}
                          >
                            <div className="flex items-center gap-3">
                              <span className="text-2xl font-black w-8">
                                {idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : `#${idx + 1}`}
                              </span>
                              <span className="font-bold">{ranking.user_name}</span>
                            </div>
                            <div className="text-right">
                              <p className="text-sm font-bold">{ranking.contribution} pts</p>
                              <p className="text-xs opacity-80">
                                Rank #{ranking.rank}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                      {selectedGroup.current_value >= selectedGroup.target_value && (
                        <div className="mt-3 text-xs text-[#8B7355] bg-white p-2 rounded">
                          <p className="font-bold">Rewards on completion:</p>
                          <p>🥇 1st: 300 pts | 🥈 2nd: 200 pts | 🥉 3rd: 150 pts</p>
                          <p className="text-[#D4A574] font-bold mt-1">
                            {rankings.length === selectedGroup.members.length ? '✨ All members bonus: +100 pts each!' : ''}
                          </p>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Messages */}
                <ScrollArea className="flex-1 p-6" data-testid="messages-area">
                  <div className="space-y-4">
                    {messages.map((message) => (
                      <div
                        key={message.id}
                        className={`flex flex-col ${
                          message.user_id === user?.id ? 'items-end' : 'items-start'
                        }`}
                        data-testid={`message-${message.id}`}
                      >
                        <p className="text-xs text-[#8B7355] mb-1">{message.user_name}</p>
                        <div
                          className={`max-w-[70%] p-3 ${
                            message.user_id === user?.id
                              ? 'bg-[#D4A574] text-black'
                              : 'bg-[#27272A] text-white'
                          }`}
                        >
                          <p>{message.content}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>

                {/* Message Input */}
                <form onSubmit={sendMessage} className="p-4 border-t border-[#D4C4B0]">
                  <div className="flex gap-2">
                    <Input
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      placeholder="Type a message..."
                      data-testid="message-input"
                      className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                    />
                    <Button
                      type="submit"
                      data-testid="send-message-btn"
                      className="bg-[#D4A574] text-black font-bold hover:bg-[#B3E600]"
                    >
                      <Send className="w-4 h-4" />
                    </Button>
                  </div>
                </form>
              </div>
            ) : (
              <div className="warm-card border border-[#D4C4B0] h-[600px] flex items-center justify-center">
                <p className="text-[#8B7355]">Select a group to view details and chat</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}