import { useNavigate } from 'react-router-dom';
import type { User } from '../../types';
import { formatDistanceToNow } from 'date-fns';
import {
  User as UserIcon,
  Shield,
  Lock,
  Unlock,
  Check,
  X,
  Crown,
  MoreVertical,
} from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

interface UserTableProps {
  users: User[];
  onQuickAction?: (userId: string, action: 'enable' | 'disable' | 'lock' | 'unlock') => void;
}

export default function UserTable({ users, onQuickAction }: UserTableProps) {
  const navigate = useNavigate();
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setOpenMenuId(null);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleRowClick = (userId: string) => {
    navigate(`/admin/users/${userId}`);
  };

  const toggleMenu = (userId: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setOpenMenuId(openMenuId === userId ? null : userId);
  };

  const handleQuickAction = (userId: string, action: 'enable' | 'disable' | 'lock' | 'unlock', e: React.MouseEvent) => {
    e.stopPropagation();
    setOpenMenuId(null);
    onQuickAction?.(userId, action);
  };

  const formatLastLogin = (lastLoginAt: string | null) => {
    if (!lastLoginAt) return 'Never';
    try {
      return formatDistanceToNow(new Date(lastLoginAt), { addSuffix: true });
    } catch {
      return 'Invalid date';
    }
  };

  const getInitials = (user: User) => {
    const firstName = user.firstName || '';
    const lastName = user.lastName || '';
    if (firstName && lastName) {
      return `${firstName[0]}${lastName[0]}`.toUpperCase();
    }
    return user.username.substring(0, 2).toUpperCase();
  };

  if (users.length === 0) {
    return (
      <div className="text-center py-12">
        <UserIcon className="mx-auto h-12 w-12 text-slate-400" />
        <h3 className="mt-2 text-sm font-medium text-slate-900">No users found</h3>
        <p className="mt-1 text-sm text-slate-500">Try adjusting your search or filters.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
              User
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
              Roles
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
              Subscription
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
              Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
              Last Login
            </th>
            <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-slate-200">
          {users.map((user) => (
            <tr
              key={user.id}
              onClick={() => handleRowClick(user.id)}
              className="hover:bg-slate-50 cursor-pointer transition-colors"
            >
              {/* User Column */}
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <div className="flex-shrink-0 h-10 w-10">
                    <div className="h-10 w-10 rounded-full bg-gradient-to-br from-brand-500 to-brand-600 flex items-center justify-center text-white font-semibold text-sm">
                      {getInitials(user)}
                    </div>
                  </div>
                  <div className="ml-4">
                    <div className="text-sm font-medium text-slate-900">{user.username}</div>
                    <div className="text-sm text-slate-500">{user.email}</div>
                    {(user.firstName || user.lastName) && (
                      <div className="text-xs text-slate-400">
                        {user.firstName} {user.lastName}
                      </div>
                    )}
                  </div>
                </div>
              </td>

              {/* Roles Column */}
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex flex-wrap gap-1">
                  {user.roles.map((role) => (
                    <span
                      key={role}
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        role === 'ADMIN'
                          ? 'bg-purple-100 text-purple-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {role === 'ADMIN' ? <Crown className="w-3 h-3" /> : <UserIcon className="w-3 h-3" />}
                      {role}
                    </span>
                  ))}
                </div>
              </td>

              {/* Subscription Column */}
              <td className="px-6 py-4 whitespace-nowrap">
                <span
                  className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    user.subscriptionPlan === 'SUBSCRIPTION_10'
                      ? 'bg-green-100 text-green-800'
                      : user.subscriptionPlan === 'FREE_BYOK'
                      ? 'bg-yellow-100 text-yellow-800'
                      : 'bg-slate-100 text-slate-800'
                  }`}
                >
                  {user.subscriptionPlan === 'SUBSCRIPTION_10'
                    ? 'Premium'
                    : user.subscriptionPlan === 'FREE_BYOK'
                    ? 'Free + BYOK'
                    : 'Free'}
                </span>
              </td>

              {/* Status Column */}
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex flex-col gap-1">
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      user.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {user.enabled ? <Check className="w-3 h-3" /> : <X className="w-3 h-3" />}
                    {user.enabled ? 'Enabled' : 'Disabled'}
                  </span>
                  {user.locked && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
                      <Lock className="w-3 h-3" />
                      Locked
                    </span>
                  )}
                  {user.emailVerified && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      <Shield className="w-3 h-3" />
                      Verified
                    </span>
                  )}
                </div>
              </td>

              {/* Last Login Column */}
              <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                {formatLastLogin(user.lastLoginAt)}
              </td>

              {/* Actions Column */}
              <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium relative">
                <button
                  onClick={(e) => toggleMenu(user.id, e)}
                  className="text-slate-400 hover:text-slate-600 transition-colors p-1 rounded-md hover:bg-slate-100"
                >
                  <MoreVertical className="w-5 h-5" />
                </button>

                {openMenuId === user.id && (
                  <div
                    ref={menuRef}
                    className="absolute right-0 mt-2 w-48 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-10"
                  >
                    <div className="py-1">
                      <button
                        onClick={() => handleRowClick(user.id)}
                        className="block w-full text-left px-4 py-2 text-sm text-slate-700 hover:bg-slate-100"
                      >
                        View Details
                      </button>
                      {user.enabled ? (
                        <button
                          onClick={(e) => handleQuickAction(user.id, 'disable', e)}
                          className="block w-full text-left px-4 py-2 text-sm text-orange-600 hover:bg-slate-100"
                        >
                          <span className="flex items-center gap-2">
                            <X className="w-4 h-4" />
                            Disable Account
                          </span>
                        </button>
                      ) : (
                        <button
                          onClick={(e) => handleQuickAction(user.id, 'enable', e)}
                          className="block w-full text-left px-4 py-2 text-sm text-green-600 hover:bg-slate-100"
                        >
                          <span className="flex items-center gap-2">
                            <Check className="w-4 h-4" />
                            Enable Account
                          </span>
                        </button>
                      )}
                      {user.locked ? (
                        <button
                          onClick={(e) => handleQuickAction(user.id, 'unlock', e)}
                          className="block w-full text-left px-4 py-2 text-sm text-green-600 hover:bg-slate-100"
                        >
                          <span className="flex items-center gap-2">
                            <Unlock className="w-4 h-4" />
                            Unlock Account
                          </span>
                        </button>
                      ) : (
                        <button
                          onClick={(e) => handleQuickAction(user.id, 'lock', e)}
                          className="block w-full text-left px-4 py-2 text-sm text-orange-600 hover:bg-slate-100"
                        >
                          <span className="flex items-center gap-2">
                            <Lock className="w-4 h-4" />
                            Lock Account
                          </span>
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
