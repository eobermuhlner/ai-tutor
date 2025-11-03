import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, X, User, LogOut, DollarSign } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import Button from '../ui/Button';
import LanguageIcons from './LanguageIcons';

export default function Header() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const userButtonRef = useRef<HTMLDivElement>(null);

  // Close user menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        userMenuOpen &&
        userMenuRef.current &&
        userButtonRef.current &&
        !userMenuRef.current.contains(event.target as Node) &&
        !userButtonRef.current.contains(event.target as Node)
      ) {
        setUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [userMenuOpen]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    setMobileMenuOpen(false);
    setUserMenuOpen(false);
  };

  const goToProfile = () => {
    navigate('/profile');
    setUserMenuOpen(false);
    setMobileMenuOpen(false);
  };

  const isAdmin = user?.roles?.includes('ROLE_ADMIN') || false;

  const navItems = [
    { label: 'Sessions', path: '/sessions' },
    { label: 'Languages', path: '/languages' },
    { label: 'Vocabulary', path: '/vocabulary' },
    { label: 'Error Patterns', path: '/error-patterns' },
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
              <div className="relative" ref={userMenuRef}>
                <div
                  ref={userButtonRef}
                  onClick={() => setUserMenuOpen(!userMenuOpen)}
                  className="flex items-center gap-2 cursor-pointer px-3 py-1.5 rounded-full bg-slate-100 hover:bg-slate-200 transition-colors"
                >
                  <User className="w-4 h-4 text-slate-600" />
                  <span className="text-sm text-slate-700 font-medium">{user.username}</span>
                  <svg
                    className={`w-4 h-4 text-slate-500 transition-transform ${userMenuOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </div>

                {/* User Dropdown Menu */}
                {userMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 rounded-lg bg-white shadow-lg border border-slate-200 py-2 z-50">
                    <button
                      onClick={goToProfile}
                      className="flex items-center w-full px-4 py-2 text-sm text-left text-slate-700 hover:bg-slate-100 transition-colors"
                    >
                      <User className="w-4 h-4 mr-2" />
                      Profile
                    </button>
                    <button
                      onClick={() => {
                        navigate('/subscription');
                        setUserMenuOpen(false);
                        setMobileMenuOpen(false);
                      }}
                      className="flex items-center w-full px-4 py-2 text-sm text-left text-slate-700 hover:bg-slate-100 transition-colors"
                    >
                      <DollarSign className="w-4 h-4 mr-2" />
                      Subscription Plan
                    </button>
                    <button
                      onClick={handleLogout}
                      className="flex items-center w-full px-4 py-2 text-sm text-left text-red-600 hover:bg-red-50 transition-colors"
                    >
                      <LogOut className="w-4 h-4 mr-2" />
                      Logout
                    </button>
                  </div>
                )}
              </div>
              <LanguageIcons userId={user.id} />
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
              <div className="pt-4 border-t border-slate-200 flex flex-col gap-3 px-4">
                <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                  <User className="w-5 h-5 text-slate-600" />
                  <span className="text-sm text-slate-700 font-medium">{user.username}</span>
                </div>
                <button
                  onClick={goToProfile}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-2"
                >
                  <User className="w-4 h-4" />
                  Profile
                </button>
                <button
                  onClick={() => {
                    navigate('/subscription');
                    setUserMenuOpen(false);
                    setMobileMenuOpen(false);
                  }}
                  className="w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-2"
                >
                  <DollarSign className="w-4 h-4" />
                  Subscription Plan
                </button>
                <Button variant="ghost" size="sm" onClick={handleLogout} className="w-full justify-start py-2 px-4 text-sm text-red-600 hover:text-red-700 hover:bg-red-50">
                  <LogOut className="w-4 h-4 mr-2" />
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
