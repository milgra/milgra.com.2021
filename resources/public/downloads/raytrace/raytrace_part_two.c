#include <stdio.h>
#include <stdint.h>		// for uint8 and uint32
#include <fcntl.h>		// for open()
#include <math.h>		// acosf, sqrtf
#include <float.h>		// for FLT_MAX
#include <sys/mman.h>	// for mmap()
#include <sys/ioctl.h>	// for framebuffer access
#include <linux/fb.h>	// for framebuffer access

typedef struct _v3_t
{
	float x, y, z;
} v3_t;

/* add two vectors */

v3_t v3_add( v3_t a , v3_t b )
{
	v3_t v;
	
	v.x = a.x + b.x;
	v.y = a.y + b.y;
	v.z = a.z + b.z;
	
	return v;
}

/* substracts b from a */

v3_t v3_sub( v3_t a , v3_t b )
{
	v3_t v;
	
	v.x = a.x - b.x;
	v.y = a.y - b.y;
	v.z = a.z - b.z;
	
	return v;
}

/* creates dot product of two vectors */

float v3_dot( v3_t a , v3_t b )
{
	return a.x * b.x + a.y * b.y + a.z * b.z;
}

/* creates cross product of two vectors */

v3_t v3_cross( v3_t left , v3_t right)
{
	v3_t v;
	
	v.x = left.y * right.z - left.z * right.y;
	v.y = left.z * right.x - left.x * right.z;
	v.z = left.x * right.y - left.y * right.x;

	return v;
}

/* scales vector */

v3_t v3_scale( v3_t a , float f )
{
	v3_t v;

	v.x = a.x * f;
	v.y = a.y * f;
	v.z = a.z * f;
	
	return v;
}

/* returns squared length to avoid square root operation */

float v3_length( v3_t a )
{
	return sqrtf( a.x * a.x + a.y * a.y + a.z * a.z );
}

/* return angle between a and b in radians */

float v3_angle( v3_t a , v3_t b )
{

	return acosf( v3_dot( a, b ) / ( v3_length( a ) * v3_length( b ) ) );

}

/* resizes vector to desired length */

v3_t v3_resize( v3_t a , float length )
{
	float ratio = length / v3_length( a );
	v3_t v = v3_scale( a , ratio );

	return v;
}

/* returns squared length to avoid square root operation */

float v3_length_squared( v3_t a )
{
	return a.x * a.x + a.y * a.y + a.z * a.z;
}

/* creates pixel color suitable for the actual screen info */

uint32_t pixel_color( uint8_t r, uint8_t g, uint8_t b, struct fb_var_screeninfo *vinfo )
{
	return 	( r << vinfo->red.offset ) | 
			( g << vinfo->green.offset ) | 
			( b << vinfo->blue.offset );
}

v3_t line_plane_intersection( v3_t line_a_p , v3_t line_b_p , v3_t plane_p , v3_t plane_n )
{

	// line - plane intersection point calculation with dot products :
	// C = A + dot(AP,N) / dot( AB,N) * AB

	v3_t is = { FLT_MAX , FLT_MAX , FLT_MAX };

	v3_t AB = v3_sub( line_b_p , line_a_p );
	v3_t AP = v3_sub( plane_p , line_a_p );

	float dotABN = v3_dot( AB , plane_n );
	float dotAPN = v3_dot( AP , plane_n );

	if ( dotABN < FLT_MIN * 10.0 || dotABN > - FLT_MIN * 10.0 )
	{
		// if dot product is not close to zero there is an intersection 

		float scale_f = dotAPN / dotABN;
		is = v3_add( line_a_p , v3_scale( AB , scale_f ) );

	}

	return is;

}

v3_t point_line_projetion( v3_t line_a_p , v3_t line_b_p , v3_t point )
{

	// project intersection point to center line of rectangle
	// C = A + dot(AP,AB) / dot(AB,AB) * AB
	
	v3_t AB = v3_sub( line_b_p , line_a_p );
	v3_t AP = v3_sub( point , line_a_p );
	
	float dotAPAB = v3_dot( AP , AB );
	float dotABAB = v3_dot( AB , AB );
	float scale_f = dotAPAB / dotABAB;

	v3_t proj_p = v3_add( line_a_p , v3_scale( AB , scale_f ) );

	return proj_p;
	
}

uint8_t *fbp;
struct fb_fix_screeninfo finfo;
struct fb_var_screeninfo vinfo;

void framebuffer_init( )
{

	int fb_fd = open( "/dev/fb0" , O_RDWR );

	// get and set screen information
	
	ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo);
	
	vinfo.grayscale = 0;
	vinfo.bits_per_pixel = 32;

	ioctl( fb_fd, FBIOPUT_VSCREENINFO, &vinfo );
	ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo );
	ioctl( fb_fd, FBIOGET_FSCREENINFO, &finfo );

	long screensize_l = vinfo.yres_virtual * finfo.line_length;

	fbp = mmap( 0 , screensize_l , PROT_READ | PROT_WRITE , MAP_SHARED , fb_fd , ( off_t ) 0 );

}

void framebuffer_drawsquare( int sx , int sy , int size , uint32_t color )
{

	for ( int y = sy ; y < sy + size ; y++  )
	{
		for ( int x = sx ; x < sx + size ; x++ )
		{

			long location = ( x + vinfo.xoffset ) * ( vinfo.bits_per_pixel / 8 ) + 
							( y + vinfo.yoffset ) * finfo.line_length;

			*( ( uint32_t* )( fbp + location ) ) = pixel_color( 
					( color >>24 ) & 0xFF , 
					( color >> 16 ) & 0xFF , 
					( color >> 8 ) & 0xFF, &vinfo );

		}
	}

}

int main( )
{

	framebuffer_init( );

	// set up camera

	v3_t camera_focus_p = { 40.0 , 20.0 , 100.0 };		// camera focus point
	v3_t camera_target_p = { 20.0 , 0.0 , 0.0 };

	// set up geometry

	v3_t rect_side_p = { -50.0 , 0.0 , -50.0 };		// left side center point of square
	v3_t rect_center_p = { 0 , 0 , -50.0 };			// center point of square
	v3_t rect_normal_v = { 0 , 0 , 100.0 };			// normal vector of square

	float rect_wth2_f = 50.0;						// half width of square
	float rect_hth2_f = 40.0;						// half height of square

	// set up light

	v3_t light_p = { 0.0 , 30.0 , 0.0 };
	
	// create corresponding grid in screen and in camera window 

    int grid_cols_i = 200;

	v3_t screen_d = { vinfo.xres , vinfo.yres };					// screen dimensions
	v3_t window_d = { 100.0 , 100.0 * screen_d.y / screen_d.x };	// camera window dimensions
	
	float screen_step_size_f = screen_d.x / grid_cols_i;	// screen block size
	float window_step_size_f = window_d.x / grid_cols_i;	// window block size

	int grid_rows_i = screen_d.y / screen_step_size_f;

	// camera window normal
	v3_t window_normal_v = v3_sub( camera_focus_p , camera_target_p );
	// xz plane normal
	v3_t xzplane_normal_v = { 0.0 , 1.0 , 0.0 };
	// create vector that is on the camera window and parallel with xz plane ( horizontal sreen axis )
	v3_t window_haxis_v  = v3_cross( window_normal_v , xzplane_normal_v );
	// create vector that is on sceeen plane and perpendicular to scr_nrm vector and prev vector ( vertical axis )
	v3_t window_vaxis_v  = v3_cross( window_normal_v , window_haxis_v );
	// resize horizontal and vertical screen vector to window step size
	v3_t window_stepx_v = v3_resize( window_haxis_v , window_step_size_f );
	v3_t window_stepy_v = v3_resize( window_vaxis_v , window_step_size_f );

    // create rays going through the camera window quad starting from the top left corner

    for ( int row_i = 0 ; row_i < grid_rows_i ; row_i++ )
    {

		for ( int col_i = 0 ; col_i < grid_cols_i ; col_i++ )
		{
		
			v3_t window_grid_v = camera_target_p;
			window_grid_v = v3_add( window_grid_v , v3_scale( window_stepx_v , grid_cols_i / 2 - col_i ) );
			window_grid_v = v3_add( window_grid_v , v3_scale( window_stepy_v , - grid_rows_i / 2 + row_i ) );

			// project ray from camera focus point through window grid point to square plane

			v3_t isect_p = line_plane_intersection( camera_focus_p , window_grid_v , rect_center_p , rect_normal_v );

			if ( isect_p.x != FLT_MAX )
			{

				// let's find if intersection point is in the rectangle

				v3_t proj_p = point_line_projetion( rect_side_p , rect_center_p , isect_p );

				// check x and y distance of intersection point from square center

				float dist_x = v3_length_squared( v3_sub( proj_p , rect_center_p ) );
				float dist_y = v3_length_squared( v3_sub( proj_p , isect_p ) );

				// ray/pixel location on screen

				int screen_grid_x = screen_step_size_f * col_i;
				int screen_grid_y = screen_step_size_f * row_i;

				// compare squared distances with squared distances of rectangle

				if ( dist_x < rect_wth2_f * rect_wth2_f && 
					 dist_y < rect_hth2_f * rect_hth2_f )
				{
					// cross point is inside square, let's calculate it's color based on light reflection angle

					// mirror light point on normal vector to get perfect reflection

					v3_t light_proj_p = point_line_projetion( isect_p , v3_add( isect_p , rect_normal_v ) , light_p );
					v3_t light_mirr_p = v3_sub( light_proj_p , light_p );

					light_mirr_p = v3_scale( light_mirr_p , 2.0 );
					light_mirr_p = v3_add( light_p , light_mirr_p );

					v3_t tofocus = v3_sub( camera_focus_p , isect_p );
					v3_t tomirrored = v3_sub( light_mirr_p , isect_p );

					float angle = v3_angle( tomirrored , tofocus );

					// the smaller the angle the closer the mirrored ray and the real ray are - reflection 

					float colorf = ( float ) 0xFF * ( ( M_PI - angle ) / M_PI );
					uint8_t coloru = ( uint8_t ) colorf;
					uint32_t color = coloru << 24 | coloru << 16 | coloru << 8 | 0xFF;

					framebuffer_drawsquare( screen_grid_x , screen_grid_y , screen_step_size_f , color );
				}
				else
				{
					// cross point is outside square, we draw it blue

					framebuffer_drawsquare( screen_grid_x , screen_grid_y , screen_step_size_f , 0x0000FFFF );
				}

			}

		}

    }

	return 0;
}
