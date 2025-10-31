import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import Button from '../ui/Button';
import LanguageIcons from './LanguageIcons';

export default function Header() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    setMobileMenuOpen(false);
  };

  const isAdmin = user?.roles?.includes('ROLE_ADMIN') || false;

  const navItems = [
    { label: 'Sessions', path: '/sessions' },
    { label: 'Languages', path: '/languages' },
    { label: 'Vocabulary', path: '/vocabulary' },
    { label: 'Error Patterns', path: '/error-patterns' },
    { label: 'Profile', path: '/profile' },
    ...(isAdmin ? [{ label: 'Admin', path: '/admin/summaries' }] : []),
  ];

  const handleNavClick = (path: string) => {
    navigate(path);
    setMobileMenuOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-slate-200 shadow-soft">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between py-4">
          {/* Logo */}
          <div
            className="cursor-pointer text-2xl font-bold bg-gradient-to-r from-brand-600 to-brand-700 bg-clip-text text-transparent hover:from-brand-700 hover:to-brand-800 transition-all"
            onClick={() => navigate('/')}
          >
            AI Tutor
          </div>

          {/* Desktop Navigation */}
          {user && (
            <nav className="hidden md:flex items-center gap-1">
              {navItems.map((item) => (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 hover:text-brand-600 hover:bg-brand-50 rounded-lg transition-all duration-200"
                >
                  {item.label}
                </button>
              ))}
            </nav>
          )}

          {/* Desktop User Menu */}
          {user && (
            <div className="hidden md:flex items-center gap-3">
              <div className="flex items-center">
                <span className="text-sm text-slate-600 px-3 py-1 bg-slate-100 rounded-full">
                  {user.username}
                </span>
                <LanguageIcons userId={user.id} />
              </div>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                Logout
              </Button>
            </div>
          )}

          {/* Mobile Menu Button */}
          {user && (
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? (
                <X className="w-6 h-6" />
              ) : (
                <Menu className="w-6 h-6" />
              )}
            </button>
          )}
        </div>

        {/* Mobile Navigation */}
        {user && mobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-slate-200 animate-in slide-in-from-top duration-200">
            <nav className="flex flex-col space-y-2">
              {navItems.map((item) => (
                <button
                  key={item.path}
                  onClick={() => handleNavClick(item.path)}
                  className="px-4 py-3 text-left text-sm font-medium text-slate-700 hover:text-brand-600 hover:bg-brand-50 rounded-lg transition-all"
                >
                  {item.label}
                </button>
              ))}
              <div className="pt-4 border-t border-slate-200 flex flex-col gap-3">
                <div className="px-4 flex items-center gap-2">
                  <span className="text-sm text-slate-600">
                    {user.username}
                  </span>
                  <LanguageIcons userId={user.id} />
                </div>
                <Button variant="ghost" size="sm" onClick={handleLogout} className="w-full">
                  Logout
                </Button>
              </div>
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
