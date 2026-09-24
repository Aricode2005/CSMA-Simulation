import pandas as pd
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend for server use
import matplotlib.pyplot as plt
import os
import sys

plt.style.use('seaborn-v0_8-darkgrid')

def plot_experiment1():
    """Experiment 1: p-Persistent CSMA — Effect of varying probability p on network performance.
    Fixed N=10 stations, each sending 100 frames. p is swept from 0.01 to 1.0."""
    try:
        df = pd.read_csv('experiment1_p_persistent.csv')
    except Exception:
        print("experiment1_p_persistent.csv not found.")
        return

    # Throughput vs p
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.plot(df['p'], df['Throughput'], 'o-', color='#58a6ff', linewidth=2, markersize=8, label='Throughput')
    ax.set_title('Experiment 1: p-Persistent CSMA — Throughput vs. Probability (p)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Probability (p)', fontsize=11)
    ax.set_ylabel('Throughput (Channel Efficiency)', fontsize=11)
    ax.set_xlim(0, 1.05)
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp1_throughput.png', dpi=150)
    plt.close(fig)

    # Delay vs p
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.plot(df['p'], df['AvgDelay'], 's-', color='#f85149', linewidth=2, markersize=8, label='Avg Delay')
    ax.set_title('Experiment 1: p-Persistent CSMA — Average Delay vs. Probability (p)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Probability (p)', fontsize=11)
    ax.set_ylabel('Average Delay (Time Slots)', fontsize=11)
    ax.set_xlim(0, 1.05)
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp1_delay.png', dpi=150)
    plt.close(fig)

    # Collisions vs p
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.bar(df['p'].astype(str), df['Collisions'], color='#d29922', alpha=0.85)
    ax.set_title('Experiment 1: p-Persistent CSMA — Collisions vs. Probability (p)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Probability (p)', fontsize=11)
    ax.set_ylabel('Total Collisions', fontsize=11)
    fig.tight_layout()
    fig.savefig('exp1_collisions.png', dpi=150)
    plt.close(fig)
    
    print("Experiment 1 plots saved.")

def plot_experiment2():
    """Experiment 2: Comparing all CSMA strategies (Non-Persistent, 1-Persistent, p-Persistent, CSMA/CD)
    as the number of stations N scales from 2 to 30."""
    try:
        df = pd.read_csv('experiment2_varying_N.csv')
    except Exception:
        print("experiment2_varying_N.csv not found.")
        return

    colors = {'Non-Persistent': '#3fb950', '1-Persistent': '#58a6ff', 'p-Persistent': '#d29922', 'CSMA/CD': '#f85149'}
    markers = {'Non-Persistent': 'o', '1-Persistent': 's', 'p-Persistent': '^', 'CSMA/CD': 'D'}

    # Throughput vs N
    fig, ax = plt.subplots(figsize=(9, 5))
    for strategy in df['Strategy'].unique():
        subset = df[df['Strategy'] == strategy]
        ax.plot(subset['N'], subset['Throughput'], marker=markers.get(strategy, 'o'),
                color=colors.get(strategy, '#888'), linewidth=2, markersize=8, label=strategy)
    ax.set_title('Experiment 2: All Strategies — Throughput vs. Number of Stations (N)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Number of Stations (N)', fontsize=11)
    ax.set_ylabel('Throughput (Channel Efficiency)', fontsize=11)
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp2_throughput.png', dpi=150)
    plt.close(fig)

    # Delay vs N
    fig, ax = plt.subplots(figsize=(9, 5))
    for strategy in df['Strategy'].unique():
        subset = df[df['Strategy'] == strategy]
        ax.plot(subset['N'], subset['AvgDelay'], marker=markers.get(strategy, 'o'),
                color=colors.get(strategy, '#888'), linewidth=2, markersize=8, label=strategy)
    ax.set_title('Experiment 2: All Strategies — Average Delay vs. Number of Stations (N)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Number of Stations (N)', fontsize=11)
    ax.set_ylabel('Average Delay (Time Slots)', fontsize=11)
    ax.legend(loc='best')
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp2_delay.png', dpi=150)
    plt.close(fig)

    # Collisions vs N
    fig, ax = plt.subplots(figsize=(9, 5))
    width = 0.2
    strategies = df['Strategy'].unique()
    n_vals = sorted(df['N'].unique())
    x = range(len(n_vals))
    for i, strategy in enumerate(strategies):
        subset = df[df['Strategy'] == strategy]
        vals = [subset[subset['N'] == n]['Collisions'].values[0] if n in subset['N'].values else 0 for n in n_vals]
        ax.bar([xi + i * width for xi in x], vals, width, label=strategy, color=colors.get(strategy, '#888'), alpha=0.85)
    ax.set_title('Experiment 2: All Strategies — Collisions vs. Number of Stations (N)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Number of Stations (N)', fontsize=11)
    ax.set_ylabel('Total Collisions', fontsize=11)
    ax.set_xticks([xi + width * 1.5 for xi in x])
    ax.set_xticklabels(n_vals)
    ax.legend(loc='best')
    fig.tight_layout()
    fig.savefig('exp2_collisions.png', dpi=150)
    plt.close(fig)

    print("Experiment 2 plots saved.")

def plot_experiment3():
    """Experiment 3: Validation of Tfr >= 2*Tp restriction.
    Plots Undetected Collisions and Throughput as Tfr varies."""
    try:
        df = pd.read_csv('experiment3_tfr_vs_tp.csv')
    except Exception:
        print("experiment3_tfr_vs_tp.csv not found.")
        return

    # Undetected Collisions vs Tfr
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.plot(df['Tfr'], df['UndetectedCollisions'], 'o-', color='#f85149', linewidth=2, markersize=8, label='Undetected Collisions')
    ax.axvline(x=20, color='gray', linestyle='--', linewidth=2, label='Threshold (2*Tp = 20)')
    ax.set_title('Experiment 3: Undetected Collisions vs. Frame Size ($T_{fr}$)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Frame Transmission Time ($T_{fr}$)', fontsize=11)
    ax.set_ylabel('Count of Undetected Collisions', fontsize=11)
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp3_undetected.png', dpi=150)
    plt.close(fig)

    # Throughput vs Tfr
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.plot(df['Tfr'], df['Throughput'], 's-', color='#3fb950', linewidth=2, markersize=8, label='Effective Throughput')
    ax.axvline(x=20, color='gray', linestyle='--', linewidth=2, label='Threshold (2*Tp = 20)')
    ax.set_title('Experiment 3: Effective Throughput vs. Frame Size ($T_{fr}$)', fontsize=13, fontweight='bold')
    ax.set_xlabel('Frame Transmission Time ($T_{fr}$)', fontsize=11)
    ax.set_ylabel('Effective Throughput', fontsize=11)
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    fig.savefig('exp3_throughput.png', dpi=150)
    plt.close(fig)
    
    print("Experiment 3 plots saved.")

if __name__ == "__main__":
    plot_experiment1()
    plot_experiment2()
    plot_experiment3()
    print("All plots generated successfully!")
