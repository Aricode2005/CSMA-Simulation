# Performance Observations of CSMA Techniques

Based on the discrete-time multithreaded simulation results, we observe the following characteristics for the CSMA strategies under varying parameters:

### 1. p-Persistent CSMA: Impact of transmission probability (p)
*(Fixed N=10 contending stations)*

- **Collisions**: As `p` increases from 0.01 to 1.0, the number of collisions grows exponentially. Higher values of `p` mean stations are more aggressive in attempting to transmit as soon as the channel becomes idle, leading to a high chance of simultaneous transmission.
- **Throughput**: Throughput typically peaks at lower values of `p` (e.g., around `p=0.10`). When `p` is too small (e.g., 0.01), throughput suffers because the channel remains idle unnecessarily (stations defer too often). When `p` is too large, throughput degrades due to massive collision overhead.
- **Transmission Delay**: At very low `p`, delay is high due to excessive deferral. At high `p`, delay is also high due to frequent collisions and subsequent exponential backoffs. The minimum delay roughly aligns with the point of maximum throughput.

### 2. Impact of Contending Stations (N) on Different Strategies
*(Varying N from 2 to 30)*

- **Non-Persistent CSMA**: 
  - *Collisions*: Remains relatively low compared to 1-persistent because stations immediately back off if the channel is busy, avoiding the "pile-up" effect at the end of a busy period.
  - *Throughput/Delay*: Yields better throughput than 1-persistent under heavy load (high N) but suffers from higher delay under light load because it doesn't transmit immediately after a busy period ends.

- **1-Persistent CSMA**: 
  - *Collisions*: Experiences a severe spike in collisions as N increases. Multiple stations waiting for the channel to become idle will all transmit simultaneously the moment it frees up.
  - *Throughput/Delay*: Good delay characteristics at low N, but efficiency plummets drastically as N grows.

- **p-Persistent CSMA (at optimal p)**:
  - Balances the aggressiveness of 1-persistent and the passiveness of non-persistent. By tuning `p` inversely proportional to N, it maintains a stable throughput and manages collisions far better than 1-persistent.

- **CSMA/CD (Collision Detection)**:
  - *Efficiency*: Highly efficient compared to basic CSMA. While the number of collision events might still be high as N increases, the penalty of a collision is drastically reduced. Instead of wasting an entire frame transmission time on a collided packet, stations abort early (detecting within the propagation window) and send a brief jam signal.
  - *Throughput*: Maintains the highest overall throughput and channel utilization among all evaluated schemes, especially under high N, because it minimizes wasted bandwidth.
