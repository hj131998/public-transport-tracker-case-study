import { describe, it, expect } from 'vitest';
import {
  crowdingLabel,
  crowdingColor,
  formatDelay,
  formatDuration,
  dataSourceBadge,
  alertBg,
  alertIcon,
  severityBadge,
} from '../utils/formatters';

describe('formatters', () => {
  describe('crowdingLabel', () => {
    it('returns Low for LOW', () => expect(crowdingLabel('LOW')).toBe('Low'));
    it('returns Moderate for MEDIUM', () => expect(crowdingLabel('MEDIUM')).toBe('Moderate'));
    it('returns Full for HIGH', () => expect(crowdingLabel('HIGH')).toBe('Full'));
  });

  describe('crowdingColor', () => {
    it('returns green for LOW', () => expect(crowdingColor('LOW')).toContain('green'));
    it('returns yellow for MEDIUM', () => expect(crowdingColor('MEDIUM')).toContain('yellow'));
    it('returns red for HIGH', () => expect(crowdingColor('HIGH')).toContain('red'));
  });

  describe('formatDelay', () => {
    it('returns On time for 0 minutes', () => expect(formatDelay(0)).toBe('On time'));
    it('returns delay string for positive minutes', () => expect(formatDelay(5)).toBe('5 min delay'));
    it('returns delay string for large delay', () => expect(formatDelay(20)).toBe('20 min delay'));
  });

  describe('formatDuration', () => {
    it('returns minutes for < 60', () => expect(formatDuration(45)).toBe('45 min'));
    it('returns hours for exactly 60', () => expect(formatDuration(60)).toBe('1h'));
    it('returns hours and minutes for > 60', () => expect(formatDuration(75)).toBe('1h 15m'));
    it('returns hours only when no remainder', () => expect(formatDuration(120)).toBe('2h'));
  });

  describe('dataSourceBadge', () => {
    it('returns LIVE label for LIVE source', () => {
      expect(dataSourceBadge('LIVE').label).toContain('LIVE');
    });
    it('returns OFFLINE label for MOCK source', () => {
      expect(dataSourceBadge('MOCK').label).toContain('OFFLINE');
    });
    it('returns STALE label for STALE source', () => {
      expect(dataSourceBadge('STALE').label).toContain('STALE');
    });
  });

  describe('alertBg', () => {
    it('returns orange classes for DELAY', () => {
      expect(alertBg('DELAY')).toBe('bg-orange-50 border-orange-400');
    });
    it('returns red classes for DISRUPTION', () => {
      expect(alertBg('DISRUPTION')).toBe('bg-red-50 border-red-400');
    });
    it('returns yellow classes for CROWDING', () => {
      expect(alertBg('CROWDING')).toBe('bg-yellow-50 border-yellow-400');
    });
    it('returns blue classes for WEATHER', () => {
      expect(alertBg('WEATHER')).toBe('bg-blue-50 border-blue-400');
    });
  });

  describe('alertIcon', () => {
    it('returns clock for DELAY', () => expect(alertIcon('DELAY')).toBe('⏱'));
    it('returns siren for DISRUPTION', () => expect(alertIcon('DISRUPTION')).toBe('🚨'));
    it('returns people for CROWDING', () => expect(alertIcon('CROWDING')).toBe('👥'));
    it('returns rain for WEATHER', () => expect(alertIcon('WEATHER')).toBe('🌧'));
  });

  describe('severityBadge', () => {
    it('returns gray classes for LOW severity', () => {
      expect(severityBadge('LOW')).toBe('bg-gray-100 text-gray-700');
    });
    it('returns yellow classes for MEDIUM severity', () => {
      expect(severityBadge('MEDIUM')).toBe('bg-yellow-100 text-yellow-800');
    });
    it('returns red classes for HIGH severity', () => {
      expect(severityBadge('HIGH')).toBe('bg-red-100 text-red-800');
    });
  });
});
