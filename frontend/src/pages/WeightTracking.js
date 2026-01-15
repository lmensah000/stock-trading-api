import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plus, Scale } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function WeightTracking() {
  const [entries, setEntries] = useState([]);
  const [newEntry, setNewEntry] = useState({
    weight: '',
    unit: 'kg',
    notes: ''
  });

  useEffect(() => {
    fetchEntries();
  }, []);

  const fetchEntries = async () => {
    try {
      const response = await apiClient.get('/weight');
      setEntries(response.data);
    } catch (error) {
      console.error('Failed to fetch weight entries:', error);
    }
  };

  const addEntry = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/weight', {
        weight: parseFloat(newEntry.weight),
        unit: newEntry.unit,
        notes: newEntry.notes || null
      });
      
      toast.success('Weight entry added!');
      setNewEntry({ weight: '', unit: 'kg', notes: '' });
      fetchEntries();
    } catch (error) {
      toast.error('Failed to add entry');
    }
  };

  const chartData = entries.slice(0, 30).reverse().map(entry => ({
    date: new Date(entry.created_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    weight: entry.weight
  }));

  return (
    <div className="min-h-screen bg-[#FAF9F7]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="weight-tracking-page">
        <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-8">
          WEIGHT <span className="text-volt">TRACKING</span>
        </h1>

        <div className="grid md:grid-cols-3 gap-8">
          {/* Add Entry Form */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="warm-card border border-[#D4C4B0] p-6"
          >
            <h2 className="text-2xl font-bold uppercase mb-6 flex items-center gap-2">
              <Scale className="w-6 h-6 text-[#D4A574]" strokeWidth={1.5} />
              LOG WEIGHT
            </h2>
            
            <form onSubmit={addEntry} className="space-y-4" data-testid="weight-form">
              <div>
                <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                  Weight
                </Label>
                <Input
                  type="number"
                  step="0.1"
                  data-testid="weight-input"
                  value={newEntry.weight}
                  onChange={(e) => setNewEntry({ ...newEntry, weight: e.target.value })}
                  className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                  required
                />
              </div>
              
              <div>
                <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                  Unit
                </Label>
                <Select
                  value={newEntry.unit}
                  onValueChange={(value) => setNewEntry({ ...newEntry, unit: value })}
                >
                  <SelectTrigger 
                    data-testid="unit-select"
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                  >
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="warm-card border-[#D4C4B0]">
                    <SelectItem value="kg" className="text-white hover:bg-[#27272A]">kg</SelectItem>
                    <SelectItem value="lbs" className="text-white hover:bg-[#27272A]">lbs</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              
              <div>
                <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                  Notes (Optional)
                </Label>
                <Textarea
                  data-testid="notes-input"
                  value={newEntry.notes}
                  onChange={(e) => setNewEntry({ ...newEntry, notes: e.target.value })}
                  placeholder="How are you feeling?"
                  className="bg-[#FAF9F7] border-[#D4C4B0] text-white focus:border-[#D4A574]"
                />
              </div>
              
              <Button
                type="submit"
                data-testid="submit-weight-btn"
                className="w-full bg-[#D4A574] text-black font-bold uppercase hover:bg-[#B3E600]"
              >
                <Plus className="w-4 h-4 mr-2" />
                ADD ENTRY
              </Button>
            </form>

            {/* Scale Integration Info */}
            <div className="mt-8 p-4 bg-[#27272A] border border-[#3F3F46]">
              <h3 className="text-sm font-bold uppercase mb-2 text-[#D4A574]">SMART SCALE SYNC</h3>
              <p className="text-xs text-[#8B7355] mb-3">
                Connect your digital scale for automatic tracking
              </p>
              <Button
                variant="outline"
                size="sm"
                data-testid="connect-scale-btn"
                className="w-full border-[#D4A574] text-[#D4A574] hover:bg-[#D4A574] hover:text-black uppercase text-xs"
              >
                CONNECT SCALE
              </Button>
              <p className="text-xs text-[#52525B] mt-2">
                Supported: Withings, Fitbit Aria, QardioBase
              </p>
            </div>
          </motion.div>

          {/* Chart & History */}
          <div className="md:col-span-2 space-y-6">
            {/* Chart */}
            {chartData.length > 0 && (
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                className="warm-card border border-[#D4C4B0] p-6"
                data-testid="weight-chart"
              >
                <h2 className="text-2xl font-bold uppercase mb-6">PROGRESS CHART</h2>
                <div className="chart-container" style={{ width: '100%', height: 300 }}>
                  <ResponsiveContainer>
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#27272A" />
                      <XAxis dataKey="date" stroke="#A1A1AA" style={{ fontSize: 12 }} />
                      <YAxis stroke="#A1A1AA" style={{ fontSize: 12 }} />
                      <Tooltip 
                        contentStyle={{ backgroundColor: '#18181B', border: '1px solid #27272A', borderRadius: '4px' }}
                        labelStyle={{ color: '#FFFFFF' }}
                        itemStyle={{ color: '#CCFF00' }}
                      />
                      <Line type="monotone" dataKey="weight" stroke="#CCFF00" strokeWidth={3} dot={{ fill: '#CCFF00', r: 5 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </motion.div>
            )}

            {/* History */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
              className="warm-card border border-[#D4C4B0] p-6"
              data-testid="weight-history"
            >
              <h2 className="text-2xl font-bold uppercase mb-6">ENTRY HISTORY</h2>
              
              {entries.length === 0 ? (
                <p className="text-[#8B7355] text-center py-8">No entries yet. Add your first weight entry!</p>
              ) : (
                <div className="space-y-3">
                  {entries.map((entry) => (
                    <div
                      key={entry.id}
                      className="flex items-center justify-between p-4 bg-[#27272A] border border-[#3F3F46]"
                      data-testid={`entry-${entry.id}`}
                    >
                      <div className="flex-1">
                        <p className="text-2xl font-bold text-[#D4A574]">
                          {entry.weight} <span className="text-lg text-[#8B7355]">{entry.unit}</span>
                        </p>
                        {entry.notes && (
                          <p className="text-sm text-[#8B7355] mt-1">{entry.notes}</p>
                        )}
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-white">
                          {new Date(entry.created_at).toLocaleDateString()}
                        </p>
                        <p className="text-xs text-[#8B7355]">
                          {new Date(entry.created_at).toLocaleTimeString()}
                        </p>
                        <span className="inline-block mt-1 text-xs uppercase font-bold tracking-widest text-[#52525B]">
                          {entry.source}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
}