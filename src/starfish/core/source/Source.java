/* *****************************************************
 * (c) 2012 Particle In Cell Consulting LLC
 * 
 * This document is subject to the license specified in 
 * Starfish.java and the LICENSE file
 * *****************************************************/
package starfish.core.source;

import starfish.core.boundaries.Spline;
import starfish.core.common.Starfish;
import starfish.core.common.Starfish.Log;
import starfish.core.materials.FluidMaterial;
import starfish.core.materials.KineticMaterial;
import starfish.core.materials.KineticMaterial.Particle;
import starfish.core.materials.Material;
import starfish.core.materials.SolidMaterial;

/** Base class for particle sources*/
public abstract class Source
{
    /*variables*/
    protected Material source_mat;
    protected double mdot0;
    protected Spline spline;
    protected String name;
   
    /**
     * constructor
     */
    public Source(String name, Material source_mat, Spline spline, double mdot)
    {
	this.source_mat = source_mat;
	this.mdot0 = mdot;
	this.spline = spline;
	this.name = name; 
    }

    /**
     * constructor for sources not associated with a spline
     */
    public Source(String name, Material source_mat)
    {
	this(name,source_mat,null,0);
    }

    /*called by simulation after add*/
    public void start()
    {
	/*do nothing*/
    }
    
    protected int num_mp;	/*number of macroparticles to sample*/
    protected double mp_rem;


    /**
     * returns true if there are more particles to sample
     */
    public boolean hasParticles()
    {
	if (num_mp > 0)
	{
	    return true;
	}
	return false;
    }

    
    double mdot(double time)
    {
//	int p=(int)(time/400e-6);
//	double t=(time-p*400e-6)/1e-6;	/*in us*/
//	return mdot0*60*Math.exp(-0.013*t)/11.47 ;
	return mdot0;
    }
    
    /**
     * default function to reset particle sample size, needs to be called prior
     * to sampling source
     */
    public void regenerate()
    {
	if (source_mat instanceof KineticMaterial)
	{
	    KineticMaterial km = (KineticMaterial) source_mat;
	    double mp =  (mdot(Starfish.time_module.getTime()) * Starfish.getDt()) / (km.getMass() * km.getSpwt0()) + mp_rem;
	    num_mp = (int) mp;
	    mp_rem = mp-num_mp;
	} else
	{
	    num_mp = 0;		/*for now?*/
	    mp_rem = 0;
	}
    }
    
    /**called prior to regenerate, allows sources to update mass flow rates, etc..*/
    public void update()
    {
	/*do nothing*/
    }

    /**
     * returns a new particle
     */
    public abstract Particle sampleParticle();

    /** samples the source for either particles or fluid*/
    final void sampleAll()
    {
	/*kinetic material*/
	if (source_mat instanceof KineticMaterial)
	{
	    sampleKinetic();
	} else if (source_mat instanceof FluidMaterial)
	{
	    sampleFluid();
	} else if (!(source_mat instanceof SolidMaterial))
	{
	//    Log.error("Attempt to sample material of type " + source_mat.getClass().getName());
	}
    }

    /**
     * samples particles from a source
     */
    void sampleKinetic()
    {
	/*source material*/
	KineticMaterial ks = (KineticMaterial) source_mat;

	/*do we have any particles?*/
	if (hasParticles() == false)
	{
	    return;
	}

	int count = 0;
	while (hasParticles())
	{
	    Particle part = sampleParticle();

	    /*only add particles located within our domain*/
	    if (part!=null && ks.addParticle(part))
	    {
		count++;
	    }
	}

	Log.log_low("Added " + count + " " + ks.getName() + " particles from " + getName());
    }

    /**
     * updates boundaries for fluid-based species
     */
    public abstract void sampleFluid();

    public Material getMaterial()
    {
	return source_mat;
    }
    
    public String getName() {return name;}
}