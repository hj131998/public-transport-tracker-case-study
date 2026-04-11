import { describe, it, expect } from 'vitest';
import {
  crowdingLabel,
  crowdingColor,
  formatDelay,
  formatDuration,
  dataSourceBadge,
  alertIcon,
} from '../../utils/formatters';

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

  describe('alertIcon', () => {
    it('returns clock for DELAY', () => expect(alertIcon('DELAY')).toBe('⏱'));
    it('returns siren for DISRUPTION', () => expect(alertIcon('DISRUPTION')).toBe('🚨'));
    it('returns people for CROWDING', () => expect(alertIcon('CROWDING')).toBe('👥'));
    it('returns rain for WEATHER', () => expect(alertIcon('WEATHER')).toBe('🌧'));
  });
});
